// @vitest-environment node
import { beforeEach, expect, test, vi } from "vitest";
import { fakeRedirect, redirectOf } from "@/test/next-fakes";
import type { AdminSession } from "./session";

const state: { session: AdminSession | null } = { session: null };
const checkAdmin = vi.fn();

vi.mock("next/navigation", () => ({ redirect: (url: string) => fakeRedirect(url) }));
vi.mock("react", async (original) => ({ ...(await original<typeof import("react")>()), cache: <T>(fn: T) => fn }));
vi.mock("./cookies", () => ({ readSession: async () => state.session }));
vi.mock("./admin-check", () => ({ checkAdmin: (token: string) => checkAdmin(token) }));

const { requireAdmin } = await import("./require-admin");

const session: AdminSession = { email: "a@b.in", accessToken: "t", refreshToken: "r", accessExpiresAt: 0, mfa: true, startedAt: 0, lastSeenAt: 0 };
const admin = { id: "u", email: "a@b.in", status: "ACTIVE" };

beforeEach(() => {
  state.session = session;
  checkAdmin.mockReset();
});

test("returns the admin when the backend confirms them", async () => {
  checkAdmin.mockResolvedValue({ kind: "admin", admin });

  expect(await requireAdmin()).toEqual(admin);
  expect(checkAdmin).toHaveBeenCalledWith("t");
});

test("no session → login; no MFA yet → authenticator code", async () => {
  state.session = null;
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
