#!/bin/bash
# =============================================================================
# build-images.sh - Builds Docker images for all services and loads into kind
# =============================================================================
#
# What this script does:
# 1. Builds Docker images for all 4 backend services + frontend
# 2. Loads images into the kind cluster (kind uses its own container registry,
#    so images built on your Mac aren't automatically available to K8s pods)
#
# Why "kind load"?
#   Docker images live in your Mac's Docker daemon. But kind runs K8s inside
#   Docker containers. The K8s nodes can't pull from your local daemon -- they
#   need images loaded into their own container runtime. "kind load" copies
#   the image from your Docker daemon into the kind node containers.
#
# Usage: ./scripts/build-images.sh [service-name]
#   No args: builds all services
#   With arg: builds only the specified service (e.g., user-service)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CLUSTER_NAME="retail-cluster"

SERVICES=(
    "user-service:services/user-service"
    "order-service:services/order-service"
    "payment-service:services/payment-service"
    "inventory-service:services/inventory-service"
    "frontend:frontend"
)

build_and_load() {
    local name=$1
    local path=$2
    local full_path="${PROJECT_DIR}/${path}"

    echo "────────────────────────────────────────"
    echo "  Building: ${name}"
    echo "  Path:     ${path}"
    echo "────────────────────────────────────────"

    if [ ! -f "${full_path}/Dockerfile" ]; then
        echo "ERROR: Dockerfile not found at ${full_path}/Dockerfile"
        return 1
    fi

    docker build -t "${name}:latest" "${full_path}"
    echo ""

    echo "  Loading ${name}:latest into kind cluster..."
    kind load docker-image "${name}:latest" --name "${CLUSTER_NAME}"
    echo "  Done: ${name}"
    echo ""
}

echo "============================================"
echo "  Building Docker Images"
echo "============================================"
echo ""

# Check prerequisites
if ! docker info &> /dev/null 2>&1; then
    echo "ERROR: Docker daemon is not running."
    exit 1
fi

if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    echo "ERROR: kind cluster '${CLUSTER_NAME}' not found."
    echo "  Run ./scripts/create-cluster.sh first."
    exit 1
fi

# Build specific service or all
if [ $# -gt 0 ]; then
    TARGET=$1
    FOUND=false
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        path="${entry#*:}"
        if [ "$name" = "$TARGET" ]; then
            build_and_load "$name" "$path"
            FOUND=true
            break
        fi
    done
    if [ "$FOUND" = false ]; then
        echo "ERROR: Unknown service '${TARGET}'"
        echo "  Available: user-service, order-service, payment-service, inventory-service, frontend"
        exit 1
    fi
else
    START_TIME=$(date +%s)

    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        path="${entry#*:}"
        build_and_load "$name" "$path"
    done

    END_TIME=$(date +%s)
    ELAPSED=$((END_TIME - START_TIME))

    echo "============================================"
    echo "  All images built and loaded! (${ELAPSED}s)"
    echo "============================================"
    echo ""
    echo "  Images in kind cluster:"
    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        echo "    - ${name}:latest"
    done
    echo ""
    echo "  To rebuild a single service:"
    echo "    ./scripts/build-images.sh user-service"
    echo ""
fi
