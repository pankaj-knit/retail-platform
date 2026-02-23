import type { OrderStatus, PaymentStatus, FailedEventStatus } from "@/lib/types";

const ORDER_STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  INVENTORY_RESERVED: "bg-blue-100 text-blue-800",
  PAYMENT_PENDING: "bg-yellow-100 text-yellow-800",
  PAYMENT_COMPLETED: "bg-green-100 text-green-800",
  PAYMENT_FAILED: "bg-red-100 text-red-800",
  SHIPPED: "bg-indigo-100 text-indigo-800",
  DELIVERED: "bg-green-100 text-green-800",
  CANCELLED: "bg-gray-100 text-gray-800",
};

const PAYMENT_STATUS_STYLES: Record<PaymentStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  PROCESSING: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  REFUNDED: "bg-purple-100 text-purple-800",
};

const FAILED_EVENT_STYLES: Record<FailedEventStatus, string> = {
  FAILED: "bg-red-100 text-red-800",
  RETRYING: "bg-yellow-100 text-yellow-800",
  RESOLVED: "bg-green-100 text-green-800",
  DISCARDED: "bg-gray-100 text-gray-800",
};

type StatusType = OrderStatus | PaymentStatus | FailedEventStatus;

export function StatusBadge({
  status,
  type = "order",
}: {
  status: StatusType;
  type?: "order" | "payment" | "failedEvent";
}) {
  const styles =
    type === "payment"
      ? PAYMENT_STATUS_STYLES
      : type === "failedEvent"
        ? FAILED_EVENT_STYLES
        : ORDER_STATUS_STYLES;

  const className =
    (styles as Record<string, string>)[status] ?? "bg-gray-100 text-gray-800";

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${className}`}
    >
      {status.replace(/_/g, " ")}
    </span>
  );
}
