"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ShoppingCart, ArrowLeft, Minus, Plus } from "lucide-react";
import { api } from "@/lib/api";
import { useCart } from "@/context/CartContext";
import type { Product } from "@/lib/types";

export default function ProductDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { addItem } = useCart();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    api
      .get<Product>(`/api/products/${params.id}`)
      .then(setProduct)
      .catch(() => setProduct(null))
      .finally(() => setLoading(false));
  }, [params.id]);

  const handleAdd = () => {
    if (!product) return;
    addItem({
      productId: product.id,
      productName: product.name,
      unitPrice: product.price,
      imageUrl: product.imageUrl,
      quantity,
    });
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow-sm p-8 animate-pulse h-96" />
      </div>
    );
  }

  if (!product) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8 text-center">
        <p className="text-lg text-[var(--color-text-muted)]">Product not found</p>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <button
        onClick={() => router.back()}
        className="flex items-center gap-1 text-sm text-blue-600 hover:underline mb-6"
      >
        <ArrowLeft size={16} /> Back to results
      </button>

      <div className="bg-white rounded-lg shadow-sm p-6 md:p-8 grid md:grid-cols-2 gap-8">
        <div className="aspect-square bg-gray-50 rounded-lg flex items-center justify-center p-8">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="max-h-full max-w-full object-contain"
            />
          ) : (
            <div className="text-gray-400">No image available</div>
          )}
        </div>

        <div className="flex flex-col">
          <p className="text-xs text-[var(--color-text-muted)] uppercase tracking-wide mb-1">
            {product.category}
          </p>
          <h1 className="text-xl md:text-2xl font-medium mb-4">{product.name}</h1>
          <p className="text-sm text-[var(--color-text-muted)] mb-6 leading-relaxed">
            {product.description}
          </p>

          <div className="border-t border-gray-200 pt-4 mt-auto">
            <p className="text-2xl font-bold text-[var(--color-danger)] mb-2">
              ${product.price.toFixed(2)}
            </p>

            {product.availableStock > 0 ? (
              <p className="text-sm text-[var(--color-success)] mb-4">
                In Stock ({product.availableStock} available)
              </p>
            ) : (
              <p className="text-sm text-[var(--color-danger)] mb-4">Out of Stock</p>
            )}

            <div className="flex items-center gap-3 mb-4">
              <span className="text-sm font-medium">Qty:</span>
              <div className="flex items-center border border-gray-300 rounded-md">
                <button
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  className="px-3 py-1.5 hover:bg-gray-50 transition-colors"
                >
                  <Minus size={14} />
                </button>
                <span className="px-4 py-1.5 border-x border-gray-300 text-sm min-w-[3rem] text-center">
                  {quantity}
                </span>
                <button
                  onClick={() =>
                    setQuantity((q) => Math.min(product.availableStock, q + 1))
                  }
                  className="px-3 py-1.5 hover:bg-gray-50 transition-colors"
                >
                  <Plus size={14} />
                </button>
              </div>
            </div>

            <button
              onClick={handleAdd}
              disabled={product.availableStock <= 0}
              className="w-full flex items-center justify-center gap-2 bg-[var(--color-accent)] hover:bg-[var(--color-accent-dark)] disabled:opacity-50 disabled:cursor-not-allowed text-[var(--color-text)] font-medium py-3 px-6 rounded-md transition-colors"
            >
              <ShoppingCart size={18} />
              {added ? "Added!" : "Add to Cart"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
