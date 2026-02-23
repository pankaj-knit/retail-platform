import { NextRequest, NextResponse } from "next/server";
import { proxyRequest } from "@/lib/proxy";

const ORDER_BACKEND = process.env.ORDER_SERVICE_URL!;
const INVENTORY_BACKEND = process.env.INVENTORY_SERVICE_URL!;
const PAYMENT_BACKEND = process.env.PAYMENT_SERVICE_URL!;

/**
 * Admin routes aggregate failed events from all 3 services.
 * GET /api/admin/failed-events?service=order|inventory|payment
 * POST /api/admin/failed-events/:id/retry?service=...
 */
export async function GET(req: NextRequest) {
  const service = req.nextUrl.searchParams.get("service");
  const backend = resolveBackend(service);
  if (!backend) {
    return NextResponse.json(
      { message: "Query param 'service' required: order|inventory|payment" },
      { status: 400 },
    );
  }
  return proxyRequest(req, backend, "/api/admin");
}

export async function POST(req: NextRequest) {
  const service = req.nextUrl.searchParams.get("service");
  const backend = resolveBackend(service);
  if (!backend) {
    return NextResponse.json(
      { message: "Query param 'service' required: order|inventory|payment" },
      { status: 400 },
    );
  }
  return proxyRequest(req, backend, "/api/admin");
}

function resolveBackend(service: string | null): string | null {
  switch (service) {
    case "order":
      return ORDER_BACKEND;
    case "inventory":
      return INVENTORY_BACKEND;
    case "payment":
      return PAYMENT_BACKEND;
    default:
      return null;
  }
}
