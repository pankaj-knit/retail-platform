#!/bin/bash
# =============================================================================
# Deploy Observability Stack to kind cluster
# =============================================================================
#
# Prerequisites:
#   1. kind cluster running (./create-cluster.sh)
#   2. Namespaces created (kubectl apply -f k8s/namespaces.yaml)
#
# What this script deploys to retail-observe namespace:
#   1. OpenTelemetry Collector  - telemetry pipeline
#   2. Jaeger                   - distributed tracing UI
#   3. Prometheus               - metrics collection + storage
#   4. Loki + Promtail          - centralized log aggregation
#   5. Grafana                  - dashboards (pre-provisioned)
#   6. NetworkPolicies          - secure communication
#
# After running:
#   - Grafana:    http://localhost:30030 (admin/admin)
#   - Jaeger UI:  http://localhost:30086
#   - Prometheus: http://localhost:30090
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
OBS_DIR="$PROJECT_DIR/k8s/observability"

echo "============================================"
echo "  Step 1: Ensure Namespace"
echo "============================================"
kubectl apply -f "$PROJECT_DIR/k8s/namespaces.yaml"
echo ""

echo "============================================"
echo "  Step 2: Deploy OpenTelemetry Collector"
echo "============================================"
kubectl apply -f "$OBS_DIR/otel-collector.yaml"
echo ""

echo "============================================"
echo "  Step 3: Deploy Jaeger"
echo "============================================"
kubectl apply -f "$OBS_DIR/jaeger.yaml"
echo ""

echo "============================================"
echo "  Step 4: Deploy Prometheus"
echo "============================================"
kubectl apply -f "$OBS_DIR/prometheus.yaml"
echo ""

echo "============================================"
echo "  Step 5: Deploy Loki + Promtail"
echo "============================================"
kubectl apply -f "$OBS_DIR/loki.yaml"
echo ""

echo "============================================"
echo "  Step 6: Deploy Grafana"
echo "============================================"
kubectl apply -f "$OBS_DIR/grafana.yaml"
echo ""

echo "============================================"
echo "  Step 7: Apply NetworkPolicies"
echo "============================================"
kubectl apply -f "$OBS_DIR/network-policies.yaml"
echo ""

echo "============================================"
echo "  Step 8: Wait for Pods"
echo "============================================"
echo "Waiting for observability pods to be ready..."
kubectl wait --for=condition=ready pod -l app=otel-collector -n retail-observe --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=jaeger -n retail-observe --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=prometheus -n retail-observe --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=loki -n retail-observe --timeout=120s || true
kubectl wait --for=condition=ready pod -l app=grafana -n retail-observe --timeout=120s || true
echo ""

echo "============================================"
echo "  Observability Stack Deployed!"
echo "============================================"
echo ""
kubectl get pods -n retail-observe
echo ""
echo "Access points:"
echo "  Grafana:    http://localhost:30030  (admin/admin)"
echo "  Jaeger UI:  http://localhost:30086"
echo "  Prometheus: http://localhost:30090"
echo ""
echo "Pre-provisioned Grafana dashboards:"
echo "  - Retail Platform - Service Overview"
echo "  - Retail Platform - Failed Events Monitor"
echo ""
echo "Grafana datasources (auto-configured):"
echo "  - Prometheus (default)"
echo "  - Jaeger"
echo "  - Loki"
echo ""
