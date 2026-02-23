#!/bin/bash
# =============================================================================
# grafana-silence.sh - Activate/deactivate the "deployment-window" mute timing
# =============================================================================
#
# The "deployment-window" mute timing is pre-provisioned in Grafana's config
# (grafana.yaml) and is always visible in the UI under Alerting > Mute Timings.
#
# By default, its schedule targets year 2000 so it never matches (inactive).
# This script toggles it:
#
#   activate   → Sets the schedule to match ALL times (suppresses notifications)
#   deactivate → Resets the schedule to year 2000 (notifications resume)
#
# Usage:
#   ./scripts/grafana-silence.sh activate   [--comment "deploying v2"]
#   ./scripts/grafana-silence.sh deactivate
#
# Environment variables:
#   GRAFANA_URL      Grafana base URL (default: http://localhost:30030)
#   GRAFANA_USER     Grafana admin user (default: admin)
#   GRAFANA_PASS     Grafana admin password (default: admin)
# =============================================================================

set -euo pipefail

GRAFANA_URL="${GRAFANA_URL:-http://localhost:30030}"
GRAFANA_USER="${GRAFANA_USER:-admin}"
GRAFANA_PASS="${GRAFANA_PASS:-admin}"
GRAFANA_AUTH="${GRAFANA_USER}:${GRAFANA_PASS}"

MUTE_TIMING_NAME="deployment-window"
ACTION="${1:-help}"
shift || true

usage() {
    echo "Usage:"
    echo "  $0 activate   [--comment \"deploying v2\"]"
    echo "  $0 deactivate"
    echo ""
    echo "The 'deployment-window' mute timing is always visible in Grafana UI."
    echo "  activate   = suppress all alert notifications"
    echo "  deactivate = resume normal alert notifications"
    echo ""
    echo "Environment:"
    echo "  GRAFANA_URL   (default: http://localhost:30030)"
    echo "  GRAFANA_USER  (default: admin)"
    echo "  GRAFANA_PASS  (default: admin)"
}

# Activate: set the mute timing to match all times (00:00-23:59 every day)
activate_mute() {
    local comment="Deployment in progress"
    while [[ $# -gt 0 ]]; do
        case $1 in
            --comment) comment="$2"; shift 2 ;;
            *) shift ;;
        esac
    done

    local payload
    payload=$(cat <<EOF
{
  "name": "${MUTE_TIMING_NAME}",
  "time_intervals": [
    {
      "times": [
        { "start_time": "00:00", "end_time": "23:59" }
      ]
    }
  ]
}
EOF
)

    local response
    response=$(curl -s -w "\n%{http_code}" \
        -u "${GRAFANA_AUTH}" \
        -H "Content-Type: application/json" \
        -X PUT \
        "${GRAFANA_URL}/api/v1/provisioning/mute-timings/${MUTE_TIMING_NAME}" \
        -H "X-Disable-Provenance: true" \
        -d "${payload}")

    local http_code
    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "200" ]] || [[ "$http_code" == "202" ]]; then
        echo "  Mute timing '${MUTE_TIMING_NAME}' ACTIVATED — ${comment}"
        echo "  All alert notifications are now suppressed."
    else
        local body
        body=$(echo "$response" | sed '$d')
        echo "ERROR: Failed to activate mute timing (HTTP ${http_code})" >&2
        echo "$body" >&2
        return 1
    fi
}

# Deactivate: reset the mute timing to year 2000 so it never matches
deactivate_mute() {
    local payload
    payload=$(cat <<EOF
{
  "name": "${MUTE_TIMING_NAME}",
  "time_intervals": [
    {
      "times": [
        { "start_time": "00:00", "end_time": "00:01" }
      ],
      "years": ["2000"]
    }
  ]
}
EOF
)

    local response
    response=$(curl -s -w "\n%{http_code}" \
        -u "${GRAFANA_AUTH}" \
        -H "Content-Type: application/json" \
        -X PUT \
        "${GRAFANA_URL}/api/v1/provisioning/mute-timings/${MUTE_TIMING_NAME}" \
        -H "X-Disable-Provenance: true" \
        -d "${payload}")

    local http_code
    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "200" ]] || [[ "$http_code" == "202" ]]; then
        echo "  Mute timing '${MUTE_TIMING_NAME}' DEACTIVATED — notifications resumed."
    else
        local body
        body=$(echo "$response" | sed '$d')
        echo "  WARNING: Could not deactivate mute timing (HTTP ${http_code}). It remains active." >&2
        echo "  $body" >&2
    fi
}

case "$ACTION" in
    activate)
        activate_mute "$@"
        ;;
    deactivate)
        deactivate_mute "$@"
        ;;
    *)
        usage
        exit 1
        ;;
esac
