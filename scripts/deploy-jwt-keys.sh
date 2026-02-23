#!/bin/bash
# =============================================================================
# deploy-jwt-keys.sh - Loads RSA keys into Kubernetes Secrets
# =============================================================================
#
# Creates two K8s Secrets:
#
# 1. "jwt-keys" in retail-app namespace:
#    Contains BOTH private + public key (for User Service to sign + verify)
#
# 2. "jwt-public-key" in retail-app namespace:
#    Contains ONLY the public key (for Order/Payment/Inventory to verify)
#
# This separation ensures that only User Service can sign tokens.
# Other services can verify but never forge tokens.
#
# Prerequisites: Run ./scripts/generate-jwt-keys.sh first
# Usage: ./scripts/deploy-jwt-keys.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
KEY_DIR="${PROJECT_DIR}/config/jwt"

echo "============================================"
echo "  Deploying JWT Keys to Kubernetes"
echo "============================================"
echo ""

# Verify keys exist
if [ ! -f "${KEY_DIR}/private-key-pkcs8.pem" ] || [ ! -f "${KEY_DIR}/public-key.pem" ]; then
    echo "ERROR: Keys not found. Run ./scripts/generate-jwt-keys.sh first."
    exit 1
fi

# --- Secret 1: Both keys (for User Service) ---
echo "[1/2] Creating jwt-keys Secret (private + public)..."
kubectl create secret generic jwt-keys \
    --namespace=retail-app \
    --from-file=private-key="${KEY_DIR}/private-key-pkcs8.pem" \
    --from-file=public-key="${KEY_DIR}/public-key.pem" \
    --dry-run=client -o yaml | kubectl apply -f -

# --- Secret 2: Public key only (for other services) ---
echo "[2/2] Creating jwt-public-key Secret (public only)..."
kubectl create secret generic jwt-public-key \
    --namespace=retail-app \
    --from-file=public-key="${KEY_DIR}/public-key.pem" \
    --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "============================================"
echo "  JWT Keys deployed!"
echo ""
echo "  jwt-keys (retail-app):        private + public (User Service)"
echo "  jwt-public-key (retail-app):  public only (other services)"
echo "============================================"
