#!/bin/bash
# =============================================================================
# create-cluster.sh - Creates a local Kubernetes cluster using kind
# =============================================================================
#
# What this script does:
# 1. Checks that Docker and kind are installed
# 2. Deletes any existing cluster with the same name (clean slate)
# 3. Creates a new 3-node cluster using our config
# 4. Applies the namespace definitions
# 5. Verifies everything is running
#
# Usage: ./scripts/create-cluster.sh
# =============================================================================

set -euo pipefail

CLUSTER_NAME="retail-cluster"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  Amazon Retail App - Cluster Setup"
echo "============================================"
echo ""

# --- Step 1: Check prerequisites ---
echo "[1/5] Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed."
    echo "  Install it from: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! docker info &> /dev/null 2>&1; then
    echo "ERROR: Docker daemon is not running. Please start Docker Desktop."
    exit 1
fi

if ! command -v kind &> /dev/null; then
    echo "ERROR: kind is not installed."
    echo "  Install it with: brew install kind"
    echo "  Or visit: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    echo "ERROR: kubectl is not installed."
    echo "  Install it with: brew install kubectl"
    exit 1
fi

echo "  Docker: $(docker --version)"
echo "  kind:   $(kind version)"
echo "  kubectl: $(kubectl version --client --short 2>/dev/null || kubectl version --client)"
echo ""

# --- Step 2: Clean up existing cluster ---
echo "[2/5] Cleaning up existing cluster (if any)..."

if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    echo "  Found existing cluster '${CLUSTER_NAME}'. Deleting..."
    kind delete cluster --name "${CLUSTER_NAME}"
    echo "  Deleted."
else
    echo "  No existing cluster found. Starting fresh."
fi
echo ""

# --- Step 3: Create the cluster ---
echo "[3/5] Creating kind cluster '${CLUSTER_NAME}' (this takes 1-2 minutes)..."
echo "  Config: kind-cluster-config.yaml"
echo "  Nodes:  1 control-plane + 2 workers"
echo ""

kind create cluster \
    --name "${CLUSTER_NAME}" \
    --config "${PROJECT_DIR}/kind-cluster-config.yaml" \
    --wait 60s

echo ""

# --- Step 4: Apply namespaces ---
echo "[4/5] Creating Kubernetes namespaces..."

kubectl apply -f "${PROJECT_DIR}/k8s/namespaces.yaml"

echo ""

# --- Step 5: Verify ---
echo "[5/5] Verifying cluster setup..."
echo ""
echo "--- Cluster Nodes ---"
kubectl get nodes -o wide
echo ""
echo "--- Namespaces ---"
kubectl get namespaces
echo ""
echo "============================================"
echo "  Cluster is ready!"
echo ""
echo "  Cluster name:  ${CLUSTER_NAME}"
echo "  Context:       kind-${CLUSTER_NAME}"
echo "  Namespaces:    retail-app, retail-data, retail-observe"
echo ""
echo "  Next step: Deploy Postgres database"
echo "    ./scripts/deploy-database.sh"
echo "============================================"
