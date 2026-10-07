// @vitest-environment node
import { describe, expect, test, vi } from "vitest";
import { accessToken } from "@/test/next-fakes";
import { guard, PUBLIC_PATHS, REFRESH_MARGIN_MS, TOUCH_EVERY_MS } from "./guard";
import { ABSOLUTE_LIMIT_MS, type AdminSession, IDLE_LIMIT_MS, seal, unseal } from "./session";
import { SupabaseAuthError } from "./supabase-auth";

const SECRET = Buffer.alloc(32, 3).toString("base64");
const NOW = 10 * ABSOLUTE_LIMIT_MS;
const noRefresh = vi.fn(() => Promise.reject(new Error("refresh not expected")));
const deps = { secret: SECRET, refresh: noRefresh };

const base: AdminSession = {
  email: "admin@glide.test",
  accessToken: accessToken({ email: "admin@glide.test", aal: "aal2" }),
  refreshToken: "rt",
  accessExpiresAt: NOW + 30 * 60_000,
  mfa: true,
  startedAt: NOW - 60 * 60_000,
  lastSeenAt: NOW - 10_000,
};

const cookieFor = (session: AdminSession) => seal(session, SECRET, 60 * 60_000);

describe("who gets through", () => {
  test.each(["/", "/salons", "/admins", "/salons/123/bank", "/login-but-not-really"])(
    "logged out: %s redirects to the login page",
    async (path) => {
      expect(await guard(path, undefined, NOW, deps)).toEqual({ kind: "redirect", location: "/login", clearCookie: false });
    },
  );

  test.each(PUBLIC_PATHS)("%s is open without a login", async (path) => {
    expect(await guard(path, undefined, NOW, deps)).toEqual({ kind: "allow" });
  });

  test("a full admin login passes without touching the cookie when it was just used", async () => {
    expect(await guard("/", await cookieFor(base), NOW, deps)).toEqual({ kind: "allow" });
  });

  test("a login that hasn't done the authenticator step goes to it", async () => {
    expect(await guard("/", await cookieFor({ ...base, mfa: false }), NOW, deps)).toEqual({
      kind: "redirect",
      location: "/login/mfa",
      clearCookie: false,
    });
  });

  test("a forged or unreadable cookie ends the login", async () => {
    expect(await guard("/", "forged", NOW, deps)).toEqual({ kind: "redirect", location: "/login?expired=1", clearCookie: true });
    expect(await guard("/", await seal(base, Buffer.alloc(32, 4).toString("base64"), 60_000), NOW, deps)).toMatchObject({
      location: "/login?expired=1",
    });
  });
});

describe("session limits (DF-30)", () => {
  test("30 minutes without activity logs out", async () => {
    const idle = { ...base, lastSeenAt: NOW - IDLE_LIMIT_MS };

    expect(await guard("/", await cookieFor(idle), NOW, deps)).toEqual({ kind: "redirect", location: "/login?expired=1", clearCookie: true });
  });

  test("12 hours after logging in logs out, however active", async () => {
    const old = { ...base, startedAt: NOW - ABSOLUTE_LIMIT_MS, lastSeenAt: NOW - 1_000 };

    expect(await guard("/", await cookieFor(old), NOW, deps)).toMatchObject({ kind: "redirect", location: "/login?expired=1" });
  });

  test("activity is recorded at most once a minute, and the cookie never outlives the 12 hours", async () => {
    const decision = await guard("/", await cookieFor({ ...base, lastSeenAt: NOW - TOUCH_EVERY_MS }), NOW, deps);

    expect(decision.kind).toBe("allow");
    const cookie = (decision as { cookie: { value: string; maxAgeMs: number } }).cookie;
    expect((await unseal<AdminSession>(cookie.value, SECRET))?.lastSeenAt).toBe(NOW);
    expect(cookie.maxAgeMs).toBe(base.startedAt + ABSOLUTE_LIMIT_MS - NOW);
  });
});

describe("token refresh", () => {
  const soon = { ...base, accessExpiresAt: NOW + REFRESH_MARGIN_MS - 1 };

  test("an access token about to expire is renewed and the new one saved", async () => {
    const fresh = accessToken({ email: "admin@glide.test", aal: "aal2" }) + "x";
    const refresh = vi.fn(async () => ({ accessToken: fresh, refreshToken: "rt2", expiresAt: NOW + 3_600_000 }));

    const decision = await guard("/", await cookieFor(soon), NOW, { secret: SECRET, refresh });

    expect(refresh).toHaveBeenCalledWith("rt");
    const saved = await unseal<AdminSession>((decision as { cookie: { value: string } }).cookie.value, SECRET);
    expect(saved).toMatchObject({ accessToken: fresh, refreshToken: "rt2", startedAt: base.startedAt, mfa: true });
  });

  test("a login ended at Supabase logs out", async () => {
    const refresh = vi.fn(() => Promise.reject(new SupabaseAuthError("session_gone", 400)));

    expect(await guard("/", await cookieFor(soon), NOW, { secret: SECRET, refresh })).toMatchObject({
      kind: "redirect",
      location: "/login?expired=1",
      clearCookie: true,
    });
  });

  test("Supabase briefly down lets the page decide (the backend refuses an expired token)", async () => {
    const refresh = vi.fn(() => Promise.reject(new SupabaseAuthError("unavailable")));

    expect(await guard("/", await cookieFor(soon), NOW, { secret: SECRET, refresh })).toEqual({ kind: "allow" });
  });
});
