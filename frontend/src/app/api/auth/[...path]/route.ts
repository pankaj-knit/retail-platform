import { NextRequest } from "next/server";
import { proxyRequest } from "@/lib/proxy";

const BACKEND = process.env.USER_SERVICE_URL!;

export async function POST(req: NextRequest) {
  return proxyRequest(req, BACKEND, "/api/auth");
}

export async function GET(req: NextRequest) {
  return proxyRequest(req, BACKEND, "/api/auth");
}
