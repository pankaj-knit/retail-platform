"use client";

import Link from "next/link";
import { ShoppingCart } from "lucide-react";
import type { Product } from "@/lib/types";
import { useCart } from "@/context/CartContext";

export function ProductCard({ product }: { product: Product }) {
  const { addItem } = useCart();

  const handleAdd = (e: React.MouseEvent) => {
    e.preventDefault();
    addItem({
      productId: product.id,
      productName: product.name,
      unitPrice: product.price,
      imageUrl: product.imageUrl,
    });
  };

  return (
    <Link
      href={`/products/${product.id}`}
      className="bg-[var(--color-card)] rounded-lg shadow-sm hover:shadow-md transition-shadow overflow-hidden flex flex-col"
    >
      <div className="aspect-square bg-gray-100 flex items-center justify-center p-4">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="max-h-full max-w-full object-contain"
          />
        ) : (
          <div className="text-gray-400 text-sm">No image</div>
        )}
      </div>

      <div className="p-4 flex flex-col flex-1">
        <h3 className="text-sm font-medium line-clamp-2 mb-1">{product.name}</h3>
        <p className="text-xs text-[var(--color-text-muted)] mb-2">{product.category}</p>

        <div className="mt-auto">
          <p className="text-lg font-bold text-[var(--color-danger)]">
            ${product.price.toFixed(2)}
          </p>

          {product.availableStock > 0 ? (
            <p className="text-xs text-[var(--color-success)] mt-1">In Stock</p>
          ) : (
            <p className="text-xs text-[var(--color-danger)] mt-1">Out of Stock</p>
          )}

          <button
            onClick={handleAdd}
            disabled={product.availableStock <= 0}
            className="mt-3 w-full flex items-center justify-center gap-2 bg-[var(--color-accent)] hover:bg-[var(--color-accent-dark)] disabled:opacity-50 disabled:cursor-not-allowed text-[var(--color-text)] text-sm font-medium py-2 px-4 rounded-md transition-colors"
          >
            <ShoppingCart size={16} />
            Add to Cart
          </button>
        </div>
      </div>
    </Link>
  );
}
