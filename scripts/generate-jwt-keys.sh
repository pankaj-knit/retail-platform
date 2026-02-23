#!/bin/bash
# =============================================================================
# generate-jwt-keys.sh - Generates RSA key pair for JWT signing
# =============================================================================
#
# Why RSA (asymmetric) instead of shared secret (symmetric)?
#
#   Shared Secret (HS256):
#     - One key does both signing AND verification
#     - Every service that needs to verify tokens must have the secret
#     - If ANY service is compromised, attacker can forge tokens
#
#   RSA Key Pair (RS256):
#     - Private key: ONLY User Service has this (signs tokens)
#     - Public key:  ALL services have this (verify tokens, can't forge)
#     - If Order Service is compromised, attacker can read tokens but
#       CANNOT create fake ones -- they don't have the private key
#
# This script generates:
#   config/jwt/private-key.pem   (kept secret, only User Service gets it)
#   config/jwt/public-key.pem    (shared with all services)
#
# Usage: ./scripts/generate-jwt-keys.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
KEY_DIR="${PROJECT_DIR}/config/jwt"

echo "============================================"
echo "  Generating RSA Key Pair for JWT"
echo "============================================"
echo ""

# Create directory
mkdir -p "${KEY_DIR}"

# Check if keys already exist
if [ -f "${KEY_DIR}/private-key.pem" ]; then
    echo "WARNING: Keys already exist at ${KEY_DIR}/"
    read -p "Overwrite? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "Aborted."
        exit 0
    fi
fi

# --- Generate RSA 2048-bit private key ---
# 2048 bits is the minimum recommended for RSA.
# 4096 is more secure but slower -- for JWT signing/verification
# the performance difference is negligible.
echo "[1/3] Generating 2048-bit RSA private key..."
openssl genrsa -out "${KEY_DIR}/private-key.pem" 2048 2>/dev/null

# --- Extract public key from private key ---
echo "[2/3] Extracting public key..."
openssl rsa -in "${KEY_DIR}/private-key.pem" \
    -pubout -out "${KEY_DIR}/public-key.pem" 2>/dev/null

# --- Convert to PKCS8 format ---
# Java's standard library reads PKCS8 format natively.
# The raw PEM from openssl is in PKCS1 format which requires
# extra parsing in Java. PKCS8 avoids that.
echo "[3/3] Converting private key to PKCS8 format (Java-compatible)..."
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
    -in "${KEY_DIR}/private-key.pem" \
    -out "${KEY_DIR}/private-key-pkcs8.pem"

echo ""
echo "============================================"
echo "  Keys generated successfully!"
echo ""
echo "  Private key (SIGN - User Service only):"
echo "    ${KEY_DIR}/private-key-pkcs8.pem"
echo ""
echo "  Public key (VERIFY - all services):"
echo "    ${KEY_DIR}/public-key.pem"
echo ""
echo "  IMPORTANT: Never commit private-key*.pem to git!"
echo "  Add config/jwt/private-key*.pem to .gitignore"
echo "============================================"
