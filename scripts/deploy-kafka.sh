#!/bin/bash
# =============================================================================
# deploy-kafka.sh - Deploys Kafka + Zookeeper and provisions ALL topics
# =============================================================================
#
# What this script does:
# 1. Verifies the cluster is running
# 2. Deploys Zookeeper (Kafka depends on it, must start first)
# 3. Waits for Zookeeper to be ready
# 4. Deploys Kafka
# 5. Waits for Kafka to be ready
# 6. Creates ALL application topics (main + retry + DLT)
# 7. Verifies topics exist
#
# Topic provisioning strategy:
#   All topics are managed here at the infrastructure level.
#   Application code has autoCreateTopics = "false" -- services never
#   create topics themselves. This gives ops full control over partition
#   counts, replication, retention, and naming.
#
# Usage: ./scripts/deploy-kafka.sh
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

PARTITIONS=3
REPLICATION=1

echo "============================================"
echo "  Deploying Kafka + Zookeeper"
echo "============================================"
echo ""

# --- Step 1: Verify cluster ---
echo "[1/7] Verifying cluster connection..."
if ! kubectl cluster-info &> /dev/null; then
    echo "ERROR: Cannot connect to Kubernetes cluster."
    echo "  Run ./scripts/create-cluster.sh first."
    exit 1
fi
echo "  Connected to cluster."
echo ""

# --- Step 2: Deploy Zookeeper ---
echo "[2/7] Deploying Zookeeper..."
kubectl apply -f "${PROJECT_DIR}/k8s/kafka/zookeeper.yaml"
echo ""

# --- Step 3: Wait for Zookeeper ---
echo "[3/7] Waiting for Zookeeper to be ready..."
kubectl rollout status statefulset/zookeeper -n retail-data --timeout=120s
echo ""

# --- Step 4: Deploy Kafka ---
echo "[4/7] Deploying Kafka..."
kubectl apply -f "${PROJECT_DIR}/k8s/kafka/kafka.yaml"
echo ""

# --- Step 5: Wait for Kafka ---
echo "[5/7] Waiting for Kafka to be ready (this may take 30-60 seconds)..."
kubectl rollout status statefulset/kafka -n retail-data --timeout=180s
echo ""

# --- Step 6: Create ALL topics ---
echo "[6/7] Creating Kafka topics..."
sleep 10

create_topic() {
    local topic=$1
    echo "  Creating: ${topic}"
    kubectl exec -n retail-data kafka-0 -- \
        kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic "${topic}" \
        --partitions "${PARTITIONS}" \
        --replication-factor "${REPLICATION}" \
        --if-not-exists \
        2>/dev/null || true
}

# ─── Main business topics ───
echo ""
echo "  --- Main Topics ---"
MAIN_TOPICS=(
    "order-created"
    "inventory-reserved"
    "inventory-released"
    "payment-completed"
    "payment-failed"
)
for t in "${MAIN_TOPICS[@]}"; do create_topic "$t"; done

# ─── Retry topics (Spring Kafka @RetryableTopic) ───
# Order Service: 3 attempts = 2 retry topics (attempt 1 = main, 2 = retry-0, 3 = retry-1)
# Payment Service: 3 attempts = 2 retry topics
# Inventory Service: 4 attempts with SUFFIX_WITH_INDEX_VALUE = 3 retry topics (-0, -1, -2)
echo ""
echo "  --- Retry Topics (Order Service) ---"
ORDER_RETRY_TOPICS=(
    "inventory-reserved-retry-0"
    "inventory-reserved-retry-1"
    "payment-completed-retry-0"
    "payment-completed-retry-1"
    "payment-failed-retry-0"
    "payment-failed-retry-1"
)
for t in "${ORDER_RETRY_TOPICS[@]}"; do create_topic "$t"; done

echo ""
echo "  --- Retry Topics (Payment Service) ---"
PAYMENT_RETRY_TOPICS=(
    "order-created-retry-0"
    "order-created-retry-1"
)
for t in "${PAYMENT_RETRY_TOPICS[@]}"; do create_topic "$t"; done

echo ""
echo "  --- Retry Topics (Inventory Service) ---"
INVENTORY_RETRY_TOPICS=(
    "payment-completed-retry-0"
    "payment-completed-retry-1"
    "payment-completed-retry-2"
    "payment-failed-retry-0"
    "payment-failed-retry-1"
    "payment-failed-retry-2"
)
for t in "${INVENTORY_RETRY_TOPICS[@]}"; do create_topic "$t"; done

# ─── Dead Letter Topics ───
echo ""
echo "  --- Dead Letter Topics (DLT) ---"
DLT_TOPICS=(
    "inventory-reserved-dlt"
    "payment-completed-dlt"
    "payment-failed-dlt"
    "order-created-dlt"
)
for t in "${DLT_TOPICS[@]}"; do create_topic "$t"; done

echo ""

# --- Step 7: Verify ---
echo "[7/7] Verifying all topics..."
echo ""
kubectl exec -n retail-data kafka-0 -- \
    kafka-topics --list --bootstrap-server localhost:9092 | sort

TOTAL=$(kubectl exec -n retail-data kafka-0 -- \
    kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | wc -l | xargs)

echo ""
echo "============================================"
echo "  Kafka is ready! (${TOTAL} topics)"
echo ""
echo "  Zookeeper:  zookeeper.retail-data.svc.cluster.local:2181"
echo "  Kafka:      kafka.retail-data.svc.cluster.local:9092"
echo "  External:   localhost:30003 (for local debugging)"
echo ""
echo "  Main Topics:"
for t in "${MAIN_TOPICS[@]}"; do echo "    - ${t}"; done
echo ""
echo "  Retry + DLT topics also provisioned."
echo "  Run: kubectl exec -n retail-data kafka-0 -- kafka-topics --list --bootstrap-server localhost:9092"
echo ""
echo "  Next step: Deploy Vault"
echo "    ./scripts/deploy-vault.sh"
echo "============================================"
