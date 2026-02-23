import { NextRequest, NextResponse } from "next/server";

/**
 * Proxy intercepts /api/* requests and injects the Authorization header
 * from the x-auth-token custom header (set by the client-side fetch wrapper).
 *
 * For page routes, no server-side redirect is needed since auth checks
 * happen client-side via useAuth() hook.
 */
export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl;

  console.log(
    JSON.stringify({
      level: "info",
      msg: "request",
      method: req.method,
      path: pathname,
      ts: new Date().toISOString(),
    }),
  );

  if (pathname.startsWith("/api/")) {
    const token = req.headers.get("x-auth-token");
    if (token) {
      const headers = new Headers(req.headers);
      headers.set("Authorization", `Bearer ${token}`);
      return NextResponse.next({ request: { headers } });
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
