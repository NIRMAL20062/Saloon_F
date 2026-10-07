// @vitest-environment node
import { unstable_doesMiddlewareMatch } from "next/experimental/testing/server";
import { NextRequest } from "next/server";
import { describe, expect, test, vi } from "vitest";
import { accessToken, TEST_ENV } from "@/test/next-fakes";
import { type AdminSession, IDLE_LIMIT_MS, seal, SESSION_COOKIE } from "./auth/session";

vi.mock("./env", () => ({ getEnv: () => TEST_ENV }));

const { config, proxy } = await import("./proxy");

describe("which requests the proxy sees", () => {
  const runsOn = (url: string, headers: Record<string, string> = {}) =>
    unstable_doesMiddlewareMatch({ config, url, headers });

  test("every page and Server Action, prefetches included (an idle login must not get through with a header)", () => {
    expect(runsOn("/")).toBe(true);
    expect(runsOn("/salons/123")).toBe(true);
    expect(runsOn("/", { "next-router-prefetch": "1" })).toBe(true);
    expect(runsOn("/", { purpose: "prefetch" })).toBe(true);
    expect(runsOn("/", { "sec-purpose": "prefetch" })).toBe(true);
  });

  test("not static files or /api (an /api route must call requireAdmin() itself)", () => {
    expect(runsOn("/_next/static/chunks/app.js")).toBe(false);
    expect(runsOn("/_next/image")).toBe(false);
    expect(runsOn("/favicon.ico")).toBe(false);
    expect(runsOn("/api/anything")).toBe(false);
  });
});

const idleSession = async () => {
  const now = Date.now();
  const session: AdminSession = {
    email: "admin@glide.test",
    accessToken: accessToken({ email: "admin@glide.test", aal: "aal2" }),
    refreshToken: "rt",
    accessExpiresAt: now + 30 * 60_000,
    mfa: true,
    startedAt: now - 2 * IDLE_LIMIT_MS,
    lastSeenAt: now - IDLE_LIMIT_MS - 10 * 60_000,
  };
  return seal(session, TEST_ENV.ADMIN_SESSION_SECRET, 60 * 60_000);
};

test.each<Record<string, string>>([{ "next-router-prefetch": "1" }, { purpose: "prefetch" }, { "sec-purpose": "prefetch" }, {}])(
  "an idle login is sent to log in, also on a prefetch (%o)",
  async (headers) => {
    const request = new NextRequest("http://localhost:3000/", { headers: { ...headers, cookie: `${SESSION_COOKIE}=${await idleSession()}` } });

    const response = await proxy(request);

    expect(response.headers.get("location")).toBe("http://localhost:3000/login?expired=1");
    expect(response.headers.get("set-cookie")).toMatch(new RegExp(`${SESSION_COOKIE}=;.*Max-Age=0`, "i"));
  },
);

test("a logged-out visit to a page redirects to /login", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/salons"));

  expect(response.status).toBe(307);
  expect(response.headers.get("location")).toBe("http://localhost:3000/login");
});

test("an unreadable session cookie is deleted on the way to the login page", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/", { headers: { cookie: `${SESSION_COOKIE}=forged` } }));

  expect(response.headers.get("location")).toBe("http://localhost:3000/login?expired=1");
  expect(response.headers.get("set-cookie")).toMatch(new RegExp(`${SESSION_COOKIE}=;.*Max-Age=0`, "i"));
});

test("the login page is served with a nonce CSP", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/login"));

  expect(response.headers.get("location")).toBeNull();
  expect(response.headers.get("content-security-policy")).toMatch(/script-src 'self' 'nonce-/);
});
