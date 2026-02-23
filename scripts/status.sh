#!/bin/bash
# =============================================================================
# status.sh - Shows the health status of the entire platform
# =============================================================================
#
# Checks all namespaces, pods, services, and provides quick diagnostics.
#
# Usage: ./scripts/status.sh
# =============================================================================

set -euo pipefail

echo "╔══════════════════════════════════════════════╗"
echo "║   Retail Platform - Status Dashboard         ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

if ! kubectl cluster-info &> /dev/null 2>&1; then
    echo "ERROR: Cannot connect to Kubernetes cluster."
    echo "  Is the cluster running? Check with: kind get clusters"
    exit 1
fi

# --- Cluster Info ---
echo "┌── Cluster ──────────────────────────────────┐"
kubectl cluster-info 2>/dev/null | head -2
echo ""

# --- Namespace: retail-app ---
echo "┌── Application Services (retail-app) ────────┐"
kubectl get pods -n retail-app -o wide 2>/dev/null || echo "  No pods found."
echo ""
kubectl get svc -n retail-app 2>/dev/null || echo "  No services found."
echo ""

# --- Namespace: retail-data ---
echo "┌── Data Layer (retail-data) ─────────────────┐"
kubectl get pods -n retail-data -o wide 2>/dev/null || echo "  No pods found."
echo ""

# --- Namespace: retail-observe ---
echo "┌── Observability (retail-observe) ───────────┐"
kubectl get pods -n retail-observe -o wide 2>/dev/null || echo "  No pods found."
echo ""

# --- Namespace: istio-system ---
echo "┌── Istio (istio-system) ─────────────────────┐"
kubectl get pods -n istio-system 2>/dev/null || echo "  Istio not installed."
echo ""

# --- Health Checks ---
echo "┌── Health Checks ────────────────────────────┐"

check_health() {
    local name=$1
    local ns=$2
    local label=$3
    local ready
    ready=$(kubectl get pods -n "$ns" -l "app=$label" -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
    if echo "$ready" | grep -q "True"; then
        echo "  [OK]   ${name}"
    elif [ -z "$ready" ]; then
        echo "  [----] ${name} (not deployed)"
    else
        echo "  [FAIL] ${name} (not ready)"
    fi
}

check_health "Postgres"          "retail-data"    "postgres"
check_health "Dragonfly"         "retail-data"    "dragonfly"
check_health "Zookeeper"         "retail-data"    "zookeeper"
check_health "Kafka"             "retail-data"    "kafka"
check_health "Vault"             "retail-data"    "vault"
check_health "User Service"      "retail-app"     "user-service"
check_health "Order Service"     "retail-app"     "order-service"
check_health "Payment Service"   "retail-app"     "payment-service"
check_health "Inventory Service" "retail-app"     "inventory-service"
check_health "Frontend"          "retail-app"     "frontend"
check_health "OTel Collector"    "retail-observe" "otel-collector"
check_health "Jaeger"            "retail-observe" "jaeger"
check_health "Prometheus"        "retail-observe" "prometheus"
check_health "Loki"              "retail-observe" "loki"
check_health "Grafana"           "retail-observe" "grafana"

echo ""

# --- Access Points ---
echo "┌── Access Points ────────────────────────────┐"
echo "  Frontend:     http://localhost:30080"
echo "  Grafana:      http://localhost:30030  (admin/admin)"
echo "  Jaeger UI:    http://localhost:30086"
echo "  Prometheus:   http://localhost:30090"
echo ""

# --- Resource Usage ---
echo "┌── Node Resources ───────────────────────────┐"
kubectl top nodes 2>/dev/null || echo "  Metrics server not available (install metrics-server for resource usage)."
echo ""
