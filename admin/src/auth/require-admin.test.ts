// @vitest-environment node
import { beforeEach, expect, test, vi } from "vitest";
import { fakeRedirect, redirectOf } from "@/test/next-fakes";
import type { AdminSession } from "./session";

const state: { session: AdminSession | null; hasCookie: boolean } = { session: null, hasCookie: false };
const checkAdmin = vi.fn();

vi.mock("next/navigation", () => ({ redirect: (url: string) => fakeRedirect(url) }));
vi.mock("react", async (original) => ({ ...(await original<typeof import("react")>()), cache: <T>(fn: T) => fn }));
vi.mock("./cookies", () => ({ readSession: async () => state.session, hasSessionCookie: async () => state.hasCookie }));
vi.mock("./admin-check", () => ({ checkAdmin: (token: string) => checkAdmin(token) }));

const { requireAdmin } = await import("./require-admin");

const session: AdminSession = { email: "a@b.in", accessToken: "t", refreshToken: "r", accessExpiresAt: 0, mfa: true, startedAt: 0, lastSeenAt: 0 };
const admin = { id: "u", email: "a@b.in", status: "ACTIVE" };

beforeEach(() => {
  state.session = session;
  state.hasCookie = true;
  checkAdmin.mockReset();
});

test("returns the admin when the backend confirms them", async () => {
  checkAdmin.mockResolvedValue({ kind: "admin", admin });

  expect(await requireAdmin()).toEqual(admin);
  expect(checkAdmin).toHaveBeenCalledWith("t");
});

test("no session → login; no MFA yet → authenticator code", async () => {
  state.session = null;
  state.hasCookie = false;
  expect(await redirectOf(() => requireAdmin())).toBe("/login");

  state.session = { ...session, mfa: false };
  expect(await redirectOf(() => requireAdmin())).toBe("/login/mfa");
  expect(checkAdmin).not.toHaveBeenCalled();
});

test.each([
  ["not_admin", "/no-access"],
  ["mfa_required", "/login/mfa"],
  ["signed_out", "/login?expired=1"],
] as const)("backend says %s → %s", async (kind, url) => {
  checkAdmin.mockResolvedValue({ kind });

  expect(await redirectOf(() => requireAdmin())).toBe(url);
});

test("a cookie without a usable session (idle, too old, unreadable) → logged-out message, before asking the backend", async () => {
  state.session = null;

  expect(await redirectOf(() => requireAdmin())).toBe("/login?expired=1");
  expect(checkAdmin).not.toHaveBeenCalled();
});
