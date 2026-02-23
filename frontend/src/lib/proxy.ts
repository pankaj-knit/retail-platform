import { NextRequest, NextResponse } from "next/server";

/**
 * Generic BFF proxy: forwards the request to a backend microservice.
 * Strips the /api/<prefix> from the path and rewrites to the backend URL.
 * Passes the Authorization header through if present.
 */
export async function proxyRequest(
  req: NextRequest,
  backendBaseUrl: string,
  stripPrefix: string,
) {
  const url = new URL(req.url);
  const remainingPath = url.pathname.replace(stripPrefix, "");
  const backendPath = `/api${remainingPath}${url.search}`;
  const backendUrl = `${backendBaseUrl}${backendPath}`;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  const authHeader = req.headers.get("authorization");
  if (authHeader) {
    headers["Authorization"] = authHeader;
  }

  const token = req.headers.get("x-auth-token");
  if (token && !authHeader) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const init: RequestInit = {
    method: req.method,
    headers,
  };

  if (req.method !== "GET" && req.method !== "HEAD") {
    try {
      const body = await req.text();
      if (body) init.body = body;
    } catch {
      // no body
    }
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 30_000);
  init.signal = controller.signal;

  const start = Date.now();
  try {
    const backendRes = await fetch(backendUrl, init);
    clearTimeout(timeout);
    const data = await backendRes.text();
    const duration = Date.now() - start;

    console.log(
      JSON.stringify({
        level: backendRes.ok ? "info" : "warn",
        msg: "proxy",
        method: req.method,
        path: url.pathname,
        backend: backendUrl,
        status: backendRes.status,
        duration_ms: duration,
      }),
    );

    return new NextResponse(data, {
      status: backendRes.status,
      headers: {
        "Content-Type": backendRes.headers.get("Content-Type") ?? "application/json",
      },
    });
  } catch (error) {
    clearTimeout(timeout);
    const duration = Date.now() - start;
    const isTimeout = error instanceof DOMException && error.name === "AbortError";
    console.error(
      JSON.stringify({
        level: "error",
        msg: "proxy_error",
        method: req.method,
        path: url.pathname,
        backend: backendUrl,
        error: isTimeout ? "timeout" : String(error),
        duration_ms: duration,
      }),
    );
    return NextResponse.json(
      { message: isTimeout ? "Request timeout" : "Service unavailable" },
      { status: isTimeout ? 504 : 503 },
    );
  }
}
