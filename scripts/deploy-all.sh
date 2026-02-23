#!/bin/bash
# =============================================================================
# deploy-all.sh - One-command deployment of the entire retail platform
# =============================================================================
#
# This is the master orchestration script. It calls every other script in
# the correct order, with proper waits between stages.
#
# Deployment order (dependencies flow top-to-bottom):
#
#   1. Create kind cluster + namespaces
#   2. Generate JWT RSA keys
#   3. Deploy Postgres + Dragonfly (data layer)
#   4. Deploy Kafka + provision topics (data layer)
#   5. Deploy Vault + store secrets (data layer)
#   6. Build Docker images + load into kind
#   7. Deploy application services (auto-silences Grafana alerts during rollout)
#   8. Deploy Istio service mesh
#   9. Deploy observability stack
#
# Total time: ~10-15 minutes (mostly Docker builds)
#
# Usage:
#   ./scripts/deploy-all.sh          # Full deployment
#   ./scripts/deploy-all.sh --skip-build  # Skip Docker builds (re-deploy only)
#   ./scripts/deploy-all.sh --from 5  # Resume from step 5
#
# Prerequisites:
#   - Docker Desktop running
#   - kind, kubectl, istioctl installed
#   - ~8GB RAM available for Docker
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIP_BUILD=false
START_FROM=1

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build) SKIP_BUILD=true; shift ;;
        --from) START_FROM=$2; shift 2 ;;
        -h|--help)
            echo "Usage: $0 [--skip-build] [--from <step>]"
            echo ""
            echo "Options:"
            echo "  --skip-build  Skip Docker image builds (use existing images)"
            echo "  --from N      Resume from step N (1-9)"
            echo ""
            echo "Steps:"
            echo "  1. Create kind cluster"
            echo "  2. Generate JWT keys"
            echo "  3. Deploy Postgres + Dragonfly"
            echo "  4. Deploy Kafka"
            echo "  5. Deploy Vault"
            echo "  6. Build Docker images"
            echo "  7. Deploy application services"
            echo "  8. Deploy Istio"
            echo "  9. Deploy observability"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

TOTAL_START=$(date +%s)

echo "╔══════════════════════════════════════════════╗"
echo "║   Retail Platform - Full Stack Deployment    ║"
echo "╠══════════════════════════════════════════════╣"
echo "║   Steps: 1-9  |  ETA: ~10-15 minutes        ║"
echo "║   Starting from step: ${START_FROM}                      ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

run_step() {
    local step_num=$1
    local step_name=$2
    local script=$3

    if [ "$step_num" -lt "$START_FROM" ]; then
        echo "⏭  Step ${step_num}: ${step_name} (skipped - resuming from step ${START_FROM})"
        echo ""
        return 0
    fi

    echo "┌──────────────────────────────────────────────┐"
    echo "│  Step ${step_num}/9: ${step_name}"
    echo "└──────────────────────────────────────────────┘"
    echo ""

    local step_start=$(date +%s)

    if bash "${SCRIPT_DIR}/${script}"; then
        local step_end=$(date +%s)
        local step_elapsed=$((step_end - step_start))
        echo ""
        echo "  Step ${step_num} completed in ${step_elapsed}s"
        echo ""
    else
        echo ""
        echo "ERROR: Step ${step_num} (${step_name}) failed!"
        echo "  Fix the issue and re-run with: $0 --from ${step_num}"
        exit 1
    fi
}

# ─── Step 1: Cluster ───
run_step 1 "Create kind cluster" "create-cluster.sh"

# ─── Step 2: JWT Keys ───
if [ "$START_FROM" -le 2 ]; then
    echo "┌──────────────────────────────────────────────┐"
    echo "│  Step 2/9: Generate JWT RSA keys"
    echo "└──────────────────────────────────────────────┘"
    echo ""

    KEY_DIR="${SCRIPT_DIR}/../config/jwt"
    if [ -f "${KEY_DIR}/private-key-pkcs8.pem" ]; then
        echo "  JWT keys already exist. Skipping generation."
    else
        # Run non-interactively (auto-generate without prompting)
        mkdir -p "${KEY_DIR}"
        openssl genrsa -out "${KEY_DIR}/private-key.pem" 2048 2>/dev/null
        openssl rsa -in "${KEY_DIR}/private-key.pem" -pubout -out "${KEY_DIR}/public-key.pem" 2>/dev/null
        openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
            -in "${KEY_DIR}/private-key.pem" -out "${KEY_DIR}/private-key-pkcs8.pem"
        echo "  JWT keys generated."
    fi
    echo ""
else
    echo "⏭  Step 2: Generate JWT RSA keys (skipped)"
    echo ""
fi

# ─── Step 3: Postgres + Dragonfly ───
run_step 3 "Deploy Postgres + Dragonfly" "deploy-database.sh"

# ─── Step 4: Kafka ───
run_step 4 "Deploy Kafka + topics" "deploy-kafka.sh"

# ─── Step 5: Vault ───
run_step 5 "Deploy Vault + secrets" "deploy-vault.sh"

# ─── Step 6: Docker Images ───
if [ "$START_FROM" -le 6 ]; then
    if [ "$SKIP_BUILD" = true ]; then
        echo "⏭  Step 6: Build Docker images (skipped - --skip-build)"
        echo ""
    else
        run_step 6 "Build Docker images" "build-images.sh"
    fi
else
    echo "⏭  Step 6: Build Docker images (skipped)"
    echo ""
fi

# ─── Step 7: Application Services ───
run_step 7 "Deploy application services" "deploy-services.sh"

# ─── Step 8: Istio ───
run_step 8 "Deploy Istio service mesh" "deploy-istio.sh"

# ─── Step 9: Observability ───
run_step 9 "Deploy observability stack" "deploy-observability.sh"

# ─── Summary ───
TOTAL_END=$(date +%s)
TOTAL_ELAPSED=$((TOTAL_END - TOTAL_START))
MINUTES=$((TOTAL_ELAPSED / 60))
SECONDS=$((TOTAL_ELAPSED % 60))

echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║   Deployment Complete!  (${MINUTES}m ${SECONDS}s)              ║"
echo "╠══════════════════════════════════════════════╣"
echo "║                                              ║"
echo "║   Application                                ║"
echo "║   ───────────                                ║"
echo "║   Frontend:     http://localhost:30080        ║"
echo "║   API:          http://localhost:30080/api    ║"
echo "║                                              ║"
echo "║   Observability                              ║"
echo "║   ─────────────                              ║"
echo "║   Grafana:      http://localhost:30030        ║"
echo "║                 (admin / admin)               ║"
echo "║   Jaeger UI:    http://localhost:30086        ║"
echo "║   Prometheus:   http://localhost:30090        ║"
echo "║                                              ║"
echo "║   Infrastructure                             ║"
echo "║   ──────────────                             ║"
echo "║   Vault UI:     kubectl port-forward         ║"
echo "║                 -n retail-data vault-0 8200   ║"
echo "║                                              ║"
echo "╚══════════════════════════════════════════════╝"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n retail-app       # App pods"
echo "  kubectl get pods -n retail-data      # Data pods"
echo "  kubectl get pods -n retail-observe   # Observability pods"
echo "  kubectl get pods -n istio-system     # Istio pods"
echo "  istioctl dashboard kiali             # Mesh visualization"
echo "  istioctl proxy-status                # Sidecar sync"
echo ""
echo "To tear down:"
echo "  ./scripts/teardown.sh"
echo ""
