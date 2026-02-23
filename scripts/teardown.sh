#!/bin/bash
# =============================================================================
# teardown.sh - Destroys the entire local cluster and cleans up
# =============================================================================
#
# This completely removes:
# - The kind Kubernetes cluster and all its nodes
# - All pods, services, volumes, and data within it
# - The kubectl context pointing to this cluster
#
# It does NOT remove:
# - Your source code or config files
# - Docker images you've built (use --prune-images to remove those)
# - JWT keys or Vault init keys in config/
#
# Usage:
#   ./scripts/teardown.sh               # Delete cluster only
#   ./scripts/teardown.sh --prune-images # Also remove Docker images
#   ./scripts/teardown.sh --full         # Delete cluster + images + generated config
# =============================================================================

set -euo pipefail

CLUSTER_NAME="retail-cluster"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

PRUNE_IMAGES=false
FULL_CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --prune-images) PRUNE_IMAGES=true; shift ;;
        --full) FULL_CLEAN=true; PRUNE_IMAGES=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

echo "============================================"
echo "  Tearing down: ${CLUSTER_NAME}"
echo "============================================"
echo ""

# --- Step 1: Delete cluster ---
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    echo "[1/3] Deleting kind cluster '${CLUSTER_NAME}'..."
    kind delete cluster --name "${CLUSTER_NAME}"
    echo "  Cluster deleted. All pods, services, volumes, and data removed."
else
    echo "[1/3] No cluster named '${CLUSTER_NAME}' found. Skipping."
fi
echo ""

# --- Step 2: Remove Docker images ---
if [ "$PRUNE_IMAGES" = true ]; then
    echo "[2/3] Removing application Docker images..."
    IMAGES=(
        "user-service:latest"
        "order-service:latest"
        "payment-service:latest"
        "inventory-service:latest"
        "frontend:latest"
    )
    for img in "${IMAGES[@]}"; do
        if docker image inspect "$img" &>/dev/null; then
            docker rmi "$img" 2>/dev/null || true
            echo "  Removed: ${img}"
        fi
    done
    echo ""
    echo "  Tip: Run 'docker system prune' to reclaim more disk space."
else
    echo "[2/3] Docker images preserved (use --prune-images to remove)."
fi
echo ""

# --- Step 3: Clean generated config ---
if [ "$FULL_CLEAN" = true ]; then
    echo "[3/3] Removing generated configuration..."
    if [ -d "${PROJECT_DIR}/config/vault" ]; then
        rm -rf "${PROJECT_DIR}/config/vault"
        echo "  Removed: config/vault/ (Vault init keys)"
    fi
    if [ -d "${PROJECT_DIR}/config/jwt" ]; then
        rm -rf "${PROJECT_DIR}/config/jwt"
        echo "  Removed: config/jwt/ (JWT RSA keys)"
    fi
else
    echo "[3/3] Generated config preserved (use --full to remove)."
fi

echo ""
echo "============================================"
echo "  Teardown complete."
echo ""
echo "  To recreate everything:"
echo "    ./scripts/deploy-all.sh"
echo "============================================"
