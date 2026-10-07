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
    expect(runsOn("/api")).toBe(false);
    expect(runsOn("/api/anything")).toBe(false);
  });

  test("pages whose name only starts like those are still covered", () => {
    expect(runsOn("/api-keys")).toBe(true);
    expect(runsOn("/apiary/1")).toBe(true);
    expect(runsOn("/favicon.ico.html")).toBe(true);
  });
});

test("a browser prefetch doesn't record activity; a page view does", async () => {
  const now = Date.now();
  const session: AdminSession = {
    email: "admin@glide.test",
    accessToken: accessToken({ email: "admin@glide.test", aal: "aal2" }),
    refreshToken: "rt",
    accessExpiresAt: now + 30 * 60_000,
    mfa: true,
    startedAt: now - 60 * 60_000,
    lastSeenAt: now - 5 * 60_000,
  };
  const cookie = `${SESSION_COOKIE}=${await seal(session, TEST_ENV.ADMIN_SESSION_SECRET, 60 * 60_000)}`;

  const prefetch = await proxy(new NextRequest("http://localhost:3000/", { headers: { cookie, "sec-purpose": "prefetch" } }));
  const view = await proxy(new NextRequest("http://localhost:3000/", { headers: { cookie } }));

  expect(prefetch.headers.get("set-cookie")).toBeNull();
  expect(view.headers.get("set-cookie")).toContain(`${SESSION_COOKIE}=`);
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

test("/login?expired=1 is served and deletes the session cookie (no loop back to /)", async () => {
  const request = new NextRequest("http://localhost:3000/login?expired=1", { headers: { cookie: `${SESSION_COOKIE}=anything` } });

  const response = await proxy(request);

  expect(response.headers.get("location")).toBeNull();
  expect(response.headers.get("set-cookie")).toMatch(new RegExp(`${SESSION_COOKIE}=;.*Max-Age=0`, "i"));
  expect(request.cookies.has(SESSION_COOKIE)).toBe(false);
});

test("the login page is served with a nonce CSP", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/login"));

  expect(response.headers.get("location")).toBeNull();
  expect(response.headers.get("content-security-policy")).toMatch(/script-src 'self' 'nonce-/);
});
