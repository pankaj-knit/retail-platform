"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { StatusBadge } from "@/components/OrderStatusBadge";
import type { OrderResponse, PaymentResponse } from "@/lib/types";

export default function OrderDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.push("/login");
      return;
    }

    setLoading(true);
    Promise.all([
      api.get<OrderResponse>(`/api/orders/${params.id}`),
      api
        .get<PaymentResponse>(`/api/payments/order/${params.id}`)
        .catch(() => null),
    ])
      .then(([orderData, paymentData]) => {
        setOrder(orderData);
        setPayment(paymentData);
      })
      .catch(() => setOrder(null))
      .finally(() => setLoading(false));
  }, [params.id, user, authLoading, router]);

  const handleCancel = async () => {
    if (!order) return;
    setCancelling(true);
    try {
      const updated = await api.post<OrderResponse>(
        `/api/orders/${order.id}/cancel`,
      );
      setOrder(updated);
    } catch {
      // error handled silently
    } finally {
      setCancelling(false);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow-sm p-8 animate-pulse h-96" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center">
        <p className="text-lg text-[var(--color-text-muted)]">Order not found</p>
      </div>
    );
  }

  const canCancel = ["PENDING", "INVENTORY_RESERVED", "PAYMENT_PENDING"].includes(
    order.status,
  );

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <button
        onClick={() => router.push("/orders")}
        className="flex items-center gap-1 text-sm text-blue-600 hover:underline mb-6"
      >
        <ArrowLeft size={16} /> Back to orders
      </button>

      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-xl font-bold">Order #{order.id}</h1>
            <p className="text-sm text-[var(--color-text-muted)] mt-1">
              Placed on {new Date(order.createdAt).toLocaleString()}
            </p>
          </div>
          <StatusBadge status={order.status} type="order" />
        </div>

        <div className="grid md:grid-cols-2 gap-6 mb-6">
          <div>
            <h3 className="text-sm font-medium text-[var(--color-text-muted)] mb-1">
              Shipping Address
            </h3>
            <p className="text-sm">{order.shippingAddress}</p>
          </div>
          {payment && (
            <div>
              <h3 className="text-sm font-medium text-[var(--color-text-muted)] mb-1">
                Payment
              </h3>
              <div className="flex items-center gap-2">
                <StatusBadge status={payment.status} type="payment" />
                {payment.transactionId && (
                  <span className="text-xs text-[var(--color-text-muted)]">
                    Txn: {payment.transactionId}
                  </span>
                )}
              </div>
              {payment.failureReason && (
                <p className="text-xs text-[var(--color-danger)] mt-1">
                  {payment.failureReason}
                </p>
              )}
            </div>
          )}
        </div>

        <h2 className="text-lg font-medium mb-3">Items</h2>
        <div className="border border-gray-200 rounded-md overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-2 font-medium">Product</th>
                <th className="text-center px-4 py-2 font-medium">Qty</th>
                <th className="text-right px-4 py-2 font-medium">Price</th>
                <th className="text-right px-4 py-2 font-medium">Subtotal</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {order.items.map((item) => (
                <tr key={item.id}>
                  <td className="px-4 py-3">{item.productName}</td>
                  <td className="px-4 py-3 text-center">{item.quantity}</td>
                  <td className="px-4 py-3 text-right">
                    ${item.unitPrice.toFixed(2)}
                  </td>
                  <td className="px-4 py-3 text-right font-medium">
                    ${item.subtotal.toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-gray-50">
              <tr>
                <td colSpan={3} className="px-4 py-3 text-right font-bold">
                  Total
                </td>
                <td className="px-4 py-3 text-right font-bold text-[var(--color-danger)]">
                  ${order.totalAmount.toFixed(2)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>

        {canCancel && (
          <div className="mt-6 flex justify-end">
            <button
              onClick={handleCancel}
              disabled={cancelling}
              className="bg-red-500 hover:bg-red-600 text-white font-medium py-2 px-6 rounded-md text-sm transition-colors disabled:opacity-50"
            >
              {cancelling ? "Cancelling..." : "Cancel Order"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
