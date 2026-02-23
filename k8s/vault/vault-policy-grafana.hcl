# =============================================================================
# Vault Policy: grafana
# =============================================================================
# This policy grants Grafana read-only access to its alerting credentials
# (Teams webhook URL, SMTP settings, email recipients).
#
# Grafana runs in retail-observe namespace and uses a Vault Agent init
# container to fetch these secrets at pod startup.
# =============================================================================

path "secret/data/grafana/alerting" {
  capabilities = ["read"]
}
