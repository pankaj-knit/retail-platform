import { NextRequest } from "next/server";
import { proxyRequest } from "@/lib/proxy";

const BACKEND = process.env.ORDER_SERVICE_URL!;

export async function GET(req: NextRequest) {
  return proxyRequest(req, BACKEND, "/api/orders");
}

export async function POST(req: NextRequest) {
  return proxyRequest(req, BACKEND, "/api/orders");
}
