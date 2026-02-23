# =============================================================================
# Vault Policy: app-service (Order, Payment, Inventory)
# =============================================================================
# This policy is shared by all non-auth services.
# They can VERIFY JWT tokens (public key) but CANNOT SIGN them (no private key).
#
# Each service also gets access to its own database credentials.
# The actual DB path is parameterized in the Vault role config, not here.
# =============================================================================

# JWT public key only -- can verify tokens, cannot forge them
path "secret/data/jwt/public-key" {
  capabilities = ["read"]
}

# Database credentials -- each service reads its own path.
# The wildcard allows: secret/data/database/order-service,
# secret/data/database/payment-service, etc.
path "secret/data/database/*" {
  capabilities = ["read"]
}
