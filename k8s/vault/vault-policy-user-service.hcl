# =============================================================================
# Vault Policy: user-service
# =============================================================================
# This policy defines what the User Service is allowed to read from Vault.
#
# User Service gets access to:
#   - JWT private key (to SIGN tokens) -- only this service has this
#   - JWT public key (to VERIFY tokens)
#   - Its own database credentials
#
# Vault policies use path-based access control:
#   path "secret/data/xyz" { capabilities = ["read"] }
#   means: "Allow reading the secret stored at path xyz"
#
# The "secret/data/" prefix is because we use the KV v2 secrets engine,
# which stores data under "secret/data/<your-path>".
# =============================================================================

# JWT private key -- ONLY User Service has this
path "secret/data/jwt/private-key" {
  capabilities = ["read"]
}

# JWT public key
path "secret/data/jwt/public-key" {
  capabilities = ["read"]
}

# Database credentials for user_db
path "secret/data/database/user-service" {
  capabilities = ["read"]
}
