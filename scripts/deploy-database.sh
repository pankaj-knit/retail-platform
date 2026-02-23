#!/bin/bash
# =============================================================================
# deploy-database.sh - Deploys Postgres and Dragonfly to the Kubernetes cluster
# =============================================================================
#
# What this script does:
# 1. Verifies the cluster and namespace exist
# 2. Applies the Secret (credentials)
# 3. Creates ConfigMap from init-databases.sql (single source of truth)
# 4. Applies the StatefulSet + Service (postgres.yaml)
# 5. Waits for Postgres to be ready
# 6. Verifies the 4 databases were created
# 7. Deploys Dragonfly (Redis-compatible L2 cache)
# 8. Waits for Dragonfly to be ready
#
# Usage: ./scripts/deploy-database.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  Deploying Postgres Database"
echo "============================================"
echo ""

# --- Step 1: Verify cluster ---
echo "[1/5] Verifying cluster connection..."
if ! kubectl cluster-info &> /dev/null; then
    echo "ERROR: Cannot connect to Kubernetes cluster."
    echo "  Run ./scripts/create-cluster.sh first."
    exit 1
fi
echo "  Connected to cluster."
echo ""

# --- Step 2: Apply Secret ---
echo "[2/6] Creating Postgres credentials (Secret)..."
kubectl apply -f "${PROJECT_DIR}/k8s/database/postgres-secret.yaml"
echo ""

# --- Step 3: Create ConfigMap from the SQL file ---
# This reads init-databases.sql and creates a ConfigMap from it.
# --from-file turns the file into a key-value pair where:
#   key   = filename (init-databases.sql)
#   value = file contents (the SQL)
# --dry-run=client + pipe to kubectl apply makes this idempotent
# (safe to run multiple times -- it creates or updates as needed).
echo "[3/6] Creating ConfigMap from init-databases.sql..."
kubectl create configmap postgres-init-scripts \
    --namespace=retail-data \
    --from-file="${PROJECT_DIR}/k8s/database/init-databases.sql" \
    --dry-run=client -o yaml | kubectl apply -f -
echo ""

# --- Step 4: Apply Postgres manifests ---
echo "[4/6] Deploying Postgres (StatefulSet + Service)..."
kubectl apply -f "${PROJECT_DIR}/k8s/database/postgres.yaml"
echo ""

# --- Step 5: Wait for readiness ---
echo "[5/6] Waiting for Postgres to be ready (this may take 30-60 seconds)..."
kubectl rollout status statefulset/postgres -n retail-data --timeout=120s
echo ""

# --- Step 6: Verify databases ---
echo "[6/6] Verifying databases were created..."
sleep 5  # Give init script a moment to complete

# Run a query inside the Postgres pod to list all databases
DATABASES=$(kubectl exec -n retail-data postgres-0 -- \
    psql -U retail_user -d postgres -t -c \
    "SELECT datname FROM pg_database WHERE datname IN ('user_db','order_db','payment_db','inventory_db') ORDER BY datname;")

echo "  Databases found:"
echo "$DATABASES" | while read -r db; do
    [ -n "$db" ] && echo "    - $(echo "$db" | xargs)"
done

echo ""
echo "  Postgres is ready!"
echo ""

# --- Step 7: Deploy Dragonfly ---
echo "[7/8] Deploying Dragonfly cache (StatefulSet + Service)..."
kubectl apply -f "${PROJECT_DIR}/k8s/database/dragonfly.yaml"
echo ""

# --- Step 8: Wait for Dragonfly ---
echo "[8/8] Waiting for Dragonfly to be ready..."
kubectl rollout status statefulset/dragonfly -n retail-data --timeout=120s
echo ""

echo "============================================"
echo "  Data layer is ready!"
echo ""
echo "  Postgres"
echo "  ────────"
echo "  Host (in-cluster): postgres.retail-data.svc.cluster.local"
echo "  Port:              5432"
echo "  User:              retail_user"
echo "  Databases:         user_db, order_db, payment_db, inventory_db"
echo ""
echo "  Dragonfly (L2 Cache)"
echo "  ────────────────────"
echo "  Host (in-cluster): dragonfly.retail-data.svc.cluster.local"
echo "  Port:              6379"
echo "  Protocol:          Redis-compatible"
echo ""
echo "  To connect manually:"
echo "    kubectl exec -it -n retail-data postgres-0 -- psql -U retail_user -d user_db"
echo "    kubectl exec -it -n retail-data dragonfly-0 -- redis-cli"
echo ""
echo "  Next step: Deploy Kafka"
echo "    ./scripts/deploy-kafka.sh"
echo "============================================"
