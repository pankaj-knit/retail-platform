#!/bin/bash
# =============================================================================
# deploy-vault.sh - Deploys and configures HashiCorp Vault
# =============================================================================
#
# This script does a LOT. Here's the full sequence:
#
#   1. Deploy Vault StatefulSet + Service to K8s
#   2. Wait for Vault pod to be running
#   3. Initialize Vault (generates unseal keys + root token)
#   4. Unseal Vault (Vault starts "sealed" -- can't read/write until unsealed)
#   5. Enable KV v2 secrets engine (key-value store for our secrets)
#   6. Store JWT keys and DB credentials in Vault
#   7. Create access policies (who can read what)
#   8. Enable Kubernetes auth method
#   9. Create roles that map K8s ServiceAccounts to Vault policies
#  10. Deploy ServiceAccounts for application pods
#
# After this script runs, any pod with the right ServiceAccount can
# authenticate to Vault and fetch its secrets -- no passwords needed.
#
# Prerequisites:
#   - Cluster running (./scripts/create-cluster.sh)
#   - JWT keys generated (./scripts/generate-jwt-keys.sh)
#
# Usage: ./scripts/deploy-vault.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
KEY_DIR="${PROJECT_DIR}/config/jwt"
VAULT_DIR="${PROJECT_DIR}/k8s/vault"

echo "============================================"
echo "  Deploying HashiCorp Vault"
echo "============================================"
echo ""

# --- Step 1: Verify prerequisites ---
echo "[1/10] Checking prerequisites..."
if ! kubectl cluster-info &> /dev/null; then
    echo "ERROR: Cluster not running. Run ./scripts/create-cluster.sh first."
    exit 1
fi
if [ ! -f "${KEY_DIR}/private-key-pkcs8.pem" ]; then
    echo "ERROR: JWT keys not found. Run ./scripts/generate-jwt-keys.sh first."
    exit 1
fi
echo "  Prerequisites OK."
echo ""

# --- Step 2: Deploy Vault ---
echo "[2/10] Deploying Vault StatefulSet + Service..."
kubectl apply -f "${VAULT_DIR}/vault.yaml"
echo ""

# --- Step 3: Wait for pod ---
echo "[3/10] Waiting for Vault pod to be running..."
kubectl wait --for=condition=Ready=false pod/vault-0 -n retail-data --timeout=120s 2>/dev/null || true
# The pod won't be "Ready" until Vault is initialized + unsealed.
# We just need it to be running (container started).
sleep 15
echo "  Vault pod is running."
echo ""

# --- Step 4: Initialize Vault ---
# Vault starts in an uninitialized state. Initialization generates:
#   - Unseal keys: Used to decrypt Vault's encryption key
#   - Root token: The initial admin token with full access
#
# We use -key-shares=1 -key-threshold=1 for simplicity.
# In production, you'd use 5 shares with a threshold of 3 (Shamir's Secret Sharing),
# meaning 3 out of 5 key holders must cooperate to unseal Vault.
echo "[4/10] Initializing Vault..."

INIT_OUTPUT=$(kubectl exec -n retail-data vault-0 -- \
    vault operator init -key-shares=1 -key-threshold=1 -format=json 2>/dev/null || echo "ALREADY_INIT")

if [ "$INIT_OUTPUT" = "ALREADY_INIT" ]; then
    echo "  Vault already initialized. Skipping."
    echo "  WARNING: Cannot retrieve unseal key and root token."
    echo "  If Vault is sealed, you'll need the previously saved keys."
else
    # Extract and save the unseal key and root token
    UNSEAL_KEY=$(echo "$INIT_OUTPUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['unseal_keys_b64'][0])")
    ROOT_TOKEN=$(echo "$INIT_OUTPUT" | python3 -c "import sys,json; print(json.load(sys.stdin)['root_token'])")

    # Save to a local file (NEVER commit this to git)
    mkdir -p "${PROJECT_DIR}/config/vault"
    cat > "${PROJECT_DIR}/config/vault/init-keys.json" <<EOF
{
  "unseal_key": "${UNSEAL_KEY}",
  "root_token": "${ROOT_TOKEN}"
}
EOF
    echo "  Vault initialized."
    echo "  Keys saved to config/vault/init-keys.json (DO NOT COMMIT TO GIT)"
fi
echo ""

# Load saved keys
if [ -f "${PROJECT_DIR}/config/vault/init-keys.json" ]; then
    UNSEAL_KEY=$(python3 -c "import json; print(json.load(open('${PROJECT_DIR}/config/vault/init-keys.json'))['unseal_key'])")
    ROOT_TOKEN=$(python3 -c "import json; print(json.load(open('${PROJECT_DIR}/config/vault/init-keys.json'))['root_token'])")
else
    echo "ERROR: Cannot find init-keys.json. Re-initialize Vault."
    exit 1
fi

# --- Step 5: Unseal Vault ---
# Vault encrypts all data with a master key. The master key itself is
# encrypted with the unseal keys. "Unsealing" provides the unseal key(s)
# so Vault can decrypt the master key and start serving requests.
#
# In production with Shamir's sharing (5 shares, threshold 3):
#   - 3 different people each provide their unseal key share
#   - Vault reconstructs the master key from the 3 shares
#   - No single person can unseal Vault alone
echo "[5/10] Unsealing Vault..."
kubectl exec -n retail-data vault-0 -- \
    vault operator unseal "$UNSEAL_KEY" > /dev/null 2>&1 || true
echo "  Vault unsealed."
echo ""

# From here on, we authenticate as root to configure Vault.
# In production, you'd create admin users and revoke the root token.
VAULT_EXEC="kubectl exec -n retail-data vault-0 -- env VAULT_TOKEN=${ROOT_TOKEN}"

# --- Step 6: Enable KV v2 secrets engine ---
# KV (Key-Value) v2 is Vault's most common secrets engine.
# v2 adds versioning: you can retrieve previous versions of a secret.
echo "[6/10] Enabling KV v2 secrets engine..."
${VAULT_EXEC} vault secrets enable -path=secret -version=2 kv 2>/dev/null || echo "  Already enabled."
echo ""

# --- Step 7: Store secrets ---
echo "[7/10] Storing secrets in Vault..."

# Store JWT private key
PRIVATE_KEY_CONTENT=$(cat "${KEY_DIR}/private-key-pkcs8.pem")
${VAULT_EXEC} vault kv put secret/jwt/private-key value="${PRIVATE_KEY_CONTENT}" > /dev/null
echo "  Stored: secret/jwt/private-key"

# Store JWT public key
PUBLIC_KEY_CONTENT=$(cat "${KEY_DIR}/public-key.pem")
${VAULT_EXEC} vault kv put secret/jwt/public-key value="${PUBLIC_KEY_CONTENT}" > /dev/null
echo "  Stored: secret/jwt/public-key"

# Store database credentials for each service
for SVC in user-service order-service payment-service inventory-service; do
    DB_NAME="${SVC//-/_}"  # user-service -> user_service
    DB_NAME="${DB_NAME%_service}_db"  # user_service -> user_db
    ${VAULT_EXEC} vault kv put "secret/database/${SVC}" \
        url="jdbc:postgresql://postgres.retail-data.svc.cluster.local:5432/${DB_NAME}" \
        username="retail_user" \
        password="retail_pass_2026" > /dev/null
    echo "  Stored: secret/database/${SVC} (db: ${DB_NAME})"
done

# Store Grafana alerting credentials (Teams webhook, SMTP, email)
# Values come from environment variables; fall back to placeholders if unset.
${VAULT_EXEC} vault kv put secret/grafana/alerting \
    teams-webhook-url="${TEAMS_WEBHOOK_URL:-https://REPLACE_WITH_YOUR_TEAMS_WEBHOOK_URL}" \
    smtp-host="${SMTP_HOST:-smtp.gmail.com:587}" \
    smtp-user="${SMTP_USER:-alerts@yourcompany.com}" \
    smtp-password="${SMTP_PASSWORD:-REPLACE_WITH_APP_PASSWORD}" \
    smtp-from-address="${SMTP_FROM:-alerts@yourcompany.com}" \
    alert-email-to="${ALERT_EMAIL_TO:-oncall-team@yourcompany.com}" > /dev/null
echo "  Stored: secret/grafana/alerting (Teams webhook + SMTP credentials)"
echo ""

# --- Step 8: Create access policies ---
echo "[8/10] Creating Vault policies..."

# Copy policy files into the Vault pod and apply them
kubectl cp "${VAULT_DIR}/vault-policy-user-service.hcl" \
    retail-data/vault-0:/tmp/user-service-policy.hcl
${VAULT_EXEC} vault policy write user-service /tmp/user-service-policy.hcl > /dev/null
echo "  Created policy: user-service (private + public key, own DB creds)"

kubectl cp "${VAULT_DIR}/vault-policy-app-service.hcl" \
    retail-data/vault-0:/tmp/app-service-policy.hcl
${VAULT_EXEC} vault policy write app-service /tmp/app-service-policy.hcl > /dev/null
echo "  Created policy: app-service (public key only, own DB creds)"

kubectl cp "${VAULT_DIR}/vault-policy-grafana.hcl" \
    retail-data/vault-0:/tmp/grafana-policy.hcl
${VAULT_EXEC} vault policy write grafana /tmp/grafana-policy.hcl > /dev/null
echo "  Created policy: grafana (alerting credentials)"
echo ""

# --- Step 9: Enable Kubernetes auth ---
# This tells Vault: "Trust K8s Service Account tokens as proof of identity."
# When a pod sends its SA token, Vault calls the K8s TokenReview API to verify it.
echo "[9/10] Configuring Kubernetes auth method..."

${VAULT_EXEC} vault auth enable kubernetes 2>/dev/null || echo "  Already enabled."

# Configure the K8s auth method with the cluster's API server address.
# Vault needs to know where to call TokenReview.
${VAULT_EXEC} vault write auth/kubernetes/config \
    kubernetes_host="https://kubernetes.default.svc.cluster.local:443" > /dev/null
echo "  Kubernetes auth configured."

# Create roles that map ServiceAccounts to policies.
# "user-service" SA in "retail-app" namespace → "user-service" policy
${VAULT_EXEC} vault write auth/kubernetes/role/user-service \
    bound_service_account_names=user-service \
    bound_service_account_namespaces=retail-app \
    policies=user-service \
    ttl=1h > /dev/null
echo "  Role: user-service SA → user-service policy"

# Other services → "app-service" policy (public key only)
for SVC in order-service payment-service inventory-service; do
    ${VAULT_EXEC} vault write "auth/kubernetes/role/${SVC}" \
        bound_service_account_names="${SVC}" \
        bound_service_account_namespaces=retail-app \
        policies=app-service \
        ttl=1h > /dev/null
    echo "  Role: ${SVC} SA → app-service policy"
done

# Grafana SA in retail-observe → "grafana" policy (alerting credentials)
${VAULT_EXEC} vault write auth/kubernetes/role/grafana \
    bound_service_account_names=grafana \
    bound_service_account_namespaces=retail-observe \
    policies=grafana \
    ttl=1h > /dev/null
echo "  Role: grafana SA (retail-observe) → grafana policy"
echo ""

# --- Step 10: Deploy ServiceAccounts ---
echo "[10/10] Creating application ServiceAccounts..."
kubectl apply -f "${VAULT_DIR}/service-accounts.yaml"

echo ""
echo "============================================"
echo "  Vault is ready!"
echo ""
echo "  Vault address:  vault.retail-data.svc.cluster.local:8200"
echo "  Root token:     ${ROOT_TOKEN}"
echo "  UI:             kubectl port-forward -n retail-data vault-0 8200:8200"
echo "                  Then open http://localhost:8200"
echo ""
echo "  Secrets stored:"
echo "    secret/jwt/private-key       (RSA private key)"
echo "    secret/jwt/public-key        (RSA public key)"
echo "    secret/database/user-service (DB credentials)"
echo "    secret/database/order-service"
echo "    secret/database/payment-service"
echo "    secret/database/inventory-service"
echo "    secret/grafana/alerting      (Teams webhook + SMTP)"
echo ""
echo "  Policies:"
echo "    user-service: private + public key, own DB creds"
echo "    app-service:  public key only, own DB creds"
echo "    grafana:      alerting credentials (Teams, SMTP)"
echo ""
echo "  K8s Auth Roles:"
echo "    user-service SA      → user-service policy"
echo "    order-service SA     → app-service policy"
echo "    payment-service SA   → app-service policy"
echo "    inventory-service SA → app-service policy"
echo "    grafana SA           → grafana policy (retail-observe)"
echo "============================================"
