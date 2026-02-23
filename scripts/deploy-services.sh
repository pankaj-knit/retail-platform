#!/bin/bash
# =============================================================================
# deploy-services.sh - Deploys all application services to Kubernetes
# =============================================================================
#
# What this script does:
# 1. Verifies cluster connection
# 2. Activates the Grafana "deployment-window" mute timing to suppress alerts
# 3. Applies NetworkPolicies for the retail-app namespace
# 4. Deploys all 4 backend microservices (user, order, payment, inventory)
# 5. Deploys the frontend
# 6. Waits for all pods to be ready
# 7. Deactivates the mute timing and verifies the deployment
#
# Prerequisites:
#   - Cluster running          (./scripts/create-cluster.sh)
#   - Postgres deployed        (./scripts/deploy-database.sh)
#   - Kafka deployed           (./scripts/deploy-kafka.sh)
#   - JWT keys generated       (./scripts/generate-jwt-keys.sh)
#   - Vault deployed           (./scripts/deploy-vault.sh)
#   - Images built + loaded    (./scripts/build-images.sh)
#
# Usage: ./scripts/deploy-services.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  Deploying Application Services"
echo "============================================"
echo ""

# --- Step 1: Verify cluster ---
echo "[1/7] Verifying cluster connection..."
if ! kubectl cluster-info &> /dev/null; then
    echo "ERROR: Cannot connect to Kubernetes cluster."
    echo "  Run ./scripts/create-cluster.sh first."
    exit 1
fi
echo "  Connected to cluster."
echo ""

# --- Step 2: Activate deployment mute timing ---
echo "[2/7] Activating deployment mute timing (suppressing alert notifications)..."
MUTE_ACTIVE=false
if "${SCRIPT_DIR}/grafana-silence.sh" activate --comment "deploy-services.sh: deploying application services" 2>/dev/null; then
    MUTE_ACTIVE=true
else
    echo "  WARNING: Could not activate mute timing (Grafana may not be running yet). Continuing."
fi
echo ""

# --- Step 3: Apply NetworkPolicies + Resource Governance ---
echo "[3/7] Applying NetworkPolicies and resource governance..."
kubectl apply -f "${PROJECT_DIR}/k8s/network-policies.yaml"
kubectl apply -f "${PROJECT_DIR}/k8s/resource-governance.yaml"
echo ""

# --- Step 4: Deploy backend services ---
echo "[4/7] Deploying backend services..."

BACKEND_SERVICES=(
    "user-service"
    "inventory-service"
    "order-service"
    "payment-service"
)

for svc in "${BACKEND_SERVICES[@]}"; do
    echo "  Deploying ${svc}..."
    kubectl apply -f "${PROJECT_DIR}/k8s/services/${svc}.yaml"
done
echo ""

# --- Step 5: Deploy frontend ---
echo "[5/7] Deploying frontend..."
kubectl apply -f "${PROJECT_DIR}/k8s/services/frontend.yaml"
echo ""

# --- Step 6: Wait for readiness ---
echo "[6/7] Waiting for all pods to be ready (timeout: 180s per service)..."
echo ""

ALL_SERVICES=("${BACKEND_SERVICES[@]}" "frontend")
FAILED=()

for svc in "${ALL_SERVICES[@]}"; do
    echo "  Waiting for ${svc}..."
    if kubectl rollout status deployment/"${svc}" -n retail-app --timeout=180s 2>/dev/null; then
        echo "    ${svc}: READY"
    else
        echo "    ${svc}: FAILED (check logs with: kubectl logs -n retail-app -l app=${svc})"
        FAILED+=("${svc}")
    fi
done
echo ""

# --- Step 7: Verify and expire silence ---
echo "[7/7] Deployment status..."
echo ""
echo "--- Pods in retail-app ---"
kubectl get pods -n retail-app -o wide
echo ""
echo "--- Services in retail-app ---"
kubectl get svc -n retail-app
echo ""

if [ ${#FAILED[@]} -gt 0 ]; then
    echo "WARNING: The following services failed to become ready:"
    for svc in "${FAILED[@]}"; do
        echo "  - ${svc}"
    done
    echo ""
    echo "Debug commands:"
    echo "  kubectl describe pod -n retail-app -l app=<service-name>"
    echo "  kubectl logs -n retail-app -l app=<service-name> --tail=50"
    echo ""
    # Deactivate mute timing on failure so alerts resume for debugging
    if [[ "${MUTE_ACTIVE}" == "true" ]]; then
        echo "Deactivating deployment mute timing (deployment failed, re-enabling alerts)..."
        "${SCRIPT_DIR}/grafana-silence.sh" deactivate 2>/dev/null || true
    fi
    exit 1
fi

# Deactivate the mute timing now that all pods are healthy
if [[ "${MUTE_ACTIVE}" == "true" ]]; then
    echo "Deactivating deployment mute timing..."
    "${SCRIPT_DIR}/grafana-silence.sh" deactivate 2>/dev/null || true
    echo ""
fi

echo "============================================"
echo "  All services deployed successfully!"
echo "============================================"
echo ""
echo "  Backend Services:"
for svc in "${BACKEND_SERVICES[@]}"; do
    echo "    - ${svc}.retail-app.svc.cluster.local:8080"
done
echo ""
echo "  Frontend:"
echo "    - frontend.retail-app.svc.cluster.local:3000"
echo ""
echo "  If Istio is installed:"
echo "    http://localhost:30080 (via Istio Ingress Gateway)"
echo ""
echo "  Next step: Deploy Istio (if not already done)"
echo "    ./scripts/deploy-istio.sh"
echo "============================================"
