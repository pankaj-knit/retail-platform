#!/bin/bash
# =============================================================================
# Deploy Istio Service Mesh to kind cluster
# =============================================================================
#
# Prerequisites:
#   1. kind cluster running (./create-cluster.sh)
#   2. istioctl installed (https://istio.io/latest/docs/setup/getting-started/#download)
#      curl -L https://istio.io/downloadIstio | sh -
#      export PATH=$PWD/istio-<version>/bin:$PATH
#
# What this script does:
#   1. Installs Istio control plane (istiod) + ingress gateway
#   2. Verifies the installation
#   3. Labels the retail-app namespace for sidecar injection
#   4. Applies mesh configuration (Gateway, VirtualServices, etc.)
#   5. Restarts existing deployments to inject sidecars
#
# After running:
#   - Access the app at http://localhost:30080
#   - All service-to-service traffic is mTLS encrypted
#   - Istio handles retries, timeouts, circuit breaking at mesh level
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ISTIO_DIR="$PROJECT_DIR/k8s/istio"

echo "============================================"
echo "  Step 1: Install Istio Control Plane"
echo "============================================"

# Check istioctl is available
if ! command -v istioctl &> /dev/null; then
    echo "ERROR: istioctl not found. Install it first:"
    echo "  curl -L https://istio.io/downloadIstio | sh -"
    echo "  export PATH=\$PWD/istio-<version>/bin:\$PATH"
    exit 1
fi

echo "Using istioctl version: $(istioctl version --remote=false 2>/dev/null || echo 'unknown')"

# Install Istio using our operator config
istioctl install -f "$ISTIO_DIR/istio-operator.yaml" -y

echo ""
echo "============================================"
echo "  Step 2: Verify Installation"
echo "============================================"

# verify-install was removed in istioctl 1.29+; fall back to pod check
istioctl verify-install 2>/dev/null || echo "(verify-install not available in this istioctl version — checking pods instead)"
echo ""
kubectl get pods -n istio-system
echo ""

echo "============================================"
echo "  Step 3: Ensure Namespace Labels"
echo "============================================"

# Ensure retail-app has sidecar injection enabled
kubectl label namespace retail-app istio-injection=enabled --overwrite
echo "retail-app namespace labeled for sidecar injection"

# Ensure retail-data does NOT have sidecar injection
kubectl label namespace retail-data istio-injection=disabled --overwrite
echo "retail-data namespace labeled to skip sidecar injection"

echo ""
echo "============================================"
echo "  Step 4: Apply Mesh Configuration"
echo "============================================"

echo "Applying PeerAuthentication (mTLS)..."
kubectl apply -f "$ISTIO_DIR/peer-authentication.yaml"

echo "Applying DestinationRules..."
kubectl apply -f "$ISTIO_DIR/destination-rules.yaml"

echo "Applying Gateway + VirtualServices..."
kubectl apply -f "$ISTIO_DIR/gateway.yaml"

echo "Applying AuthorizationPolicies..."
kubectl apply -f "$ISTIO_DIR/authorization-policies.yaml"

echo "Applying Rate Limiting..."
kubectl apply -f "$ISTIO_DIR/rate-limit.yaml"

echo "Applying Sidecar scope..."
kubectl apply -f "$ISTIO_DIR/sidecar.yaml"

echo ""
echo "============================================"
echo "  Step 5: Restart Deployments for Sidecar Injection"
echo "============================================"

# Restart all deployments in retail-app so Istio can inject sidecars
for deploy in $(kubectl get deployments -n retail-app -o name 2>/dev/null); do
    echo "Restarting $deploy..."
    kubectl rollout restart "$deploy" -n retail-app
done

echo ""
echo "Waiting for rollouts to complete..."
for deploy in $(kubectl get deployments -n retail-app -o name 2>/dev/null); do
    kubectl rollout status "$deploy" -n retail-app --timeout=120s || true
done

echo ""
echo "============================================"
echo "  Step 6: Verify Mesh"
echo "============================================"

echo ""
echo "Pods in retail-app (should show 2/2 READY for sidecar):"
kubectl get pods -n retail-app
echo ""

echo "Istio Ingress Gateway:"
kubectl get svc istio-ingressgateway -n istio-system
echo ""

echo "============================================"
echo "  Istio Deployment Complete!"
echo "============================================"
echo ""
echo "Access points:"
echo "  Frontend:  http://localhost:30080"
echo "  API:       http://localhost:30080/api/..."
echo ""
echo "Useful commands:"
echo "  istioctl dashboard kiali          # Service mesh visualization"
echo "  istioctl proxy-status             # Check sidecar sync status"
echo "  istioctl analyze -n retail-app    # Check for config issues"
echo ""
