"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "@/context/CartContext";
import {
  ShoppingCart,
  User,
  Package,
  LogOut,
  LogIn,
  Shield,
} from "lucide-react";

export function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const { totalItems } = useCart();

  return (
    <nav className="sticky top-0 z-50 bg-[var(--color-primary)] text-white h-16 flex items-center px-4 md:px-8 shadow-md">
      <Link href="/" className="text-xl font-bold text-[var(--color-accent)] mr-8 shrink-0">
        RetailHub
      </Link>

      <div className="hidden md:flex items-center flex-1 max-w-xl">
        <input
          type="text"
          placeholder="Search products..."
          className="w-full px-4 py-2 rounded-l-md text-[var(--color-text)] bg-white outline-none text-sm"
          readOnly
        />
        <button className="bg-[var(--color-accent)] hover:bg-[var(--color-accent-dark)] px-4 py-2 rounded-r-md text-[var(--color-text)] font-medium text-sm transition-colors">
          Search
        </button>
      </div>

      <div className="flex items-center gap-2 md:gap-4 ml-auto">
        {user ? (
          <>
            <Link
              href="/profile"
              className="flex items-center gap-1 hover:text-[var(--color-accent)] transition-colors text-sm"
            >
              <User size={18} />
              <span className="hidden md:inline">
                Hello, {user.firstName}
              </span>
            </Link>

            <Link
              href="/orders"
              className="flex items-center gap-1 hover:text-[var(--color-accent)] transition-colors text-sm"
            >
              <Package size={18} />
              <span className="hidden md:inline">Orders</span>
            </Link>

            {isAdmin && (
              <Link
                href="/admin/failed-events"
                className="flex items-center gap-1 hover:text-[var(--color-accent)] transition-colors text-sm"
              >
                <Shield size={18} />
                <span className="hidden md:inline">Admin</span>
              </Link>
            )}

            <button
              onClick={logout}
              className="flex items-center gap-1 hover:text-[var(--color-accent)] transition-colors text-sm"
            >
              <LogOut size={18} />
              <span className="hidden md:inline">Sign Out</span>
            </button>
          </>
        ) : (
          <Link
            href="/login"
            className="flex items-center gap-1 hover:text-[var(--color-accent)] transition-colors text-sm"
          >
            <LogIn size={18} />
            <span>Sign In</span>
          </Link>
        )}

        <Link
          href="/cart"
          className="relative flex items-center gap-1 hover:text-[var(--color-accent)] transition-colors text-sm"
        >
          <ShoppingCart size={22} />
          {totalItems > 0 && (
            <span className="absolute -top-2 -right-2 bg-[var(--color-accent)] text-[var(--color-text)] text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
              {totalItems}
            </span>
          )}
          <span className="hidden md:inline">Cart</span>
        </Link>
      </div>
    </nav>
  );
}
