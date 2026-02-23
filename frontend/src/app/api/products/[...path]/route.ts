import { NextRequest } from "next/server";
import { proxyRequest } from "@/lib/proxy";

const BACKEND = process.env.INVENTORY_SERVICE_URL!;

export async function GET(req: NextRequest) {
  return proxyRequest(req, BACKEND, "/api/products");
}
