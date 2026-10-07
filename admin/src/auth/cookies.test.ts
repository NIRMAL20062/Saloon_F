// @vitest-environment node
import { beforeEach, describe, expect, test, vi } from "vitest";
import { accessToken, createCookieJar, TEST_ENV } from "@/test/next-fakes";
import { ABSOLUTE_LIMIT_MS, type AdminSession, IDLE_LIMIT_MS, seal, SESSION_COOKIE } from "./session";

const cookies = createCookieJar();

vi.mock("next/headers", () => ({ cookies: async () => cookies.store }));
vi.mock("@/env", () => ({ getEnv: () => TEST_ENV }));

const { hasSessionCookie, readSession } = await import("./cookies");

const now = Date.now();
const active: AdminSession = {
  email: "admin@glide.test",
  accessToken: accessToken({ email: "admin@glide.test", aal: "aal2" }),
  refreshToken: "rt",
  accessExpiresAt: now + 30 * 60_000,
  mfa: true,
  startedAt: now - 60 * 60_000,
  lastSeenAt: now - 60_000,
};

const store = async (session: AdminSession) => cookies.store.set(SESSION_COOKIE, await seal(session, TEST_ENV.ADMIN_SESSION_SECRET, 60 * 60_000));

beforeEach(() => cookies.jar.clear());

describe("readSession() applies the session limits itself (DF-30, WEB-008)", () => {
  test("an active session is read", async () => {
    await store(active);

    expect(await readSession()).toMatchObject({ email: "admin@glide.test", mfa: true });
  });

  test("30 minutes without activity: no session", async () => {
    await store({ ...active, lastSeenAt: now - IDLE_LIMIT_MS });

    expect(await readSession()).toBeNull();
  });

  test("12 hours after logging in: no session, however active", async () => {
    await store({ ...active, startedAt: now - ABSOLUTE_LIMIT_MS, lastSeenAt: now });

    expect(await readSession()).toBeNull();
  });

  test("an unreadable cookie: no session", async () => {
    cookies.store.set(SESSION_COOKIE, "forged");

    expect(await readSession()).toBeNull();
  });
});

test("hasSessionCookie() tells a stale login from none", async () => {
  expect(await hasSessionCookie()).toBe(false);

  await store({ ...active, lastSeenAt: now - IDLE_LIMIT_MS });

  expect(await hasSessionCookie()).toBe(true);
});
