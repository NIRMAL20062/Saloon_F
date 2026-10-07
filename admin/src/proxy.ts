import { NextRequest, NextResponse } from "next/server";
import { getEnv } from "./env";
import { guard } from "./auth/guard";
import { cookieOptions, SESSION_COOKIE } from "./auth/session";
import { supabaseAuth } from "./auth/supabase-auth";
import { buildCsp, createNonce } from "./security/csp";

/**
 * Runs before every page, prefetches included: sends anyone without a full admin login to /login (WEB-005, DF-30),
 * keeps the session alive (activity, token refresh), and adds a fresh nonce-based Content-Security-Policy. Pages check
 * again with requireAdmin(); this is the first line, not the only one.
 */
export async function proxy(request: NextRequest) {
  const decision = await guard(
    request.nextUrl.pathname,
    request.cookies.get(SESSION_COOKIE)?.value,
    Date.now(),
    { secret: getEnv().ADMIN_SESSION_SECRET, refresh: (refreshToken) => supabaseAuth().refresh(refreshToken) },
    { prefetch: isPrefetch(request.headers) },
  );

  if (decision.kind === "redirect") {
    const response = NextResponse.redirect(new URL(decision.location, request.url));
    if (decision.clearCookie) response.cookies.set(SESSION_COOKIE, "", { ...cookieOptions(0), maxAge: 0 });
    return response;
  }

  // The page must see a renewed session in this same request, so update the request's cookie too.
  if (decision.cookie) request.cookies.set(SESSION_COOKIE, decision.cookie.value);

  const nonce = createNonce();
  const csp = buildCsp(nonce, process.env.NODE_ENV === "development");

  // Next.js reads the nonce from the request's CSP header and applies it to its own scripts.
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", csp);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", csp);
  if (decision.cookie) response.cookies.set(SESSION_COOKIE, decision.cookie.value, cookieOptions(decision.cookie.maxAgeMs));
  return response;
}

/** Next.js's router and browsers mark prefetches with one of these headers. */
function isPrefetch(headers: Headers): boolean {
  return headers.has("next-router-prefetch") || headers.get("purpose") === "prefetch" || headers.get("sec-purpose")?.startsWith("prefetch") === true;
}

export const config = {
  // Pages, Server Actions and prefetches; not static files. Not /api either: an /api route must call requireAdmin()
  // itself. Never skip requests by header: a client chooses its headers (WEB-008).
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
