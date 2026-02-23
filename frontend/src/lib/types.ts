// ─── User Service DTOs ───

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface UserProfile {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  role: string;
  createdAt: string;
}

// ─── Inventory Service DTOs ───

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  category: string;
  imageUrl: string;
  availableStock: number;
}

export interface StockCheck {
  productId: number;
  availableStock: number;
  inStock: boolean;
}

// ─── Order Service DTOs ───

export type OrderStatus =
  | "PENDING"
  | "INVENTORY_RESERVED"
  | "PAYMENT_PENDING"
  | "PAYMENT_COMPLETED"
  | "PAYMENT_FAILED"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

export interface OrderItemRequest {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  shippingAddress: string;
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  inventoryReserved: boolean;
}

export interface OrderResponse {
  id: number;
  userEmail: string;
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}

// ─── Payment Service DTOs ───

export type PaymentStatus =
  | "PENDING"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"
  | "REFUNDED";

export interface PaymentResponse {
  id: number;
  orderId: number;
  userEmail: string;
  amount: number;
  status: PaymentStatus;
  transactionId: string | null;
  failureReason: string | null;
  createdAt: string;
  completedAt: string | null;
}

// ─── Failed Events (Admin) ───

export type FailedEventStatus = "FAILED" | "RETRYING" | "RESOLVED" | "DISCARDED";

export interface FailedEvent {
  id: number;
  topic: string;
  eventKey: string;
  payload: string;
  errorMessage: string;
  status: FailedEventStatus;
  retryCount: number;
  maxRetries: number;
  createdAt: string;
  resolvedAt: string | null;
}

// ─── Pagination (Spring Data Page) ───

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ─── Cart (client-side) ───

export interface CartItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  imageUrl: string;
}
