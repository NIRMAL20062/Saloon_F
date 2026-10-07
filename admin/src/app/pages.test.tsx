import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, test, vi } from "vitest";
import type { AdminSession, PendingLogin } from "@/auth/session";
import { fakeRedirect, redirectOf } from "@/test/next-fakes";

const state: { session: AdminSession | null; pending: PendingLogin | null } = { session: null, pending: null };

vi.mock("next/navigation", () => ({ redirect: (url: string) => fakeRedirect(url) }));
vi.mock("@/auth/cookies", () => ({
  readSession: async () => state.session,
  readPendingLogin: async () => state.pending,
}));
vi.mock("@/auth/require-admin", () => ({
  requireAdmin: async () => ({ id: "u-1", email: "admin@glide.test", status: "ACTIVE" }),
}));
vi.mock("./login/actions", () => ({
  sendCode: vi.fn(),
  verifyEmailCode: vi.fn(),
  setUpAuthenticator: vi.fn(),
  verifyAuthenticator: vi.fn(),
  logout: vi.fn(),
}));

const { default: LoginPage } = await import("./login/page");
const { default: EmailCodePage } = await import("./login/code/page");
const { default: AuthenticatorCodePage } = await import("./login/mfa/page");
const { default: AuthenticatorSetupPage } = await import("./login/mfa/setup/page");
const { default: NoAccessPage } = await import("./no-access/page");
const { default: Home } = await import("./page");

const aal1: AdminSession = { email: "a@b.in", accessToken: "t", refreshToken: "r", accessExpiresAt: 0, mfa: false, startedAt: 0, lastSeenAt: 0 };
const params = (query: Record<string, string> = {}) => ({ params: Promise.resolve({}), searchParams: Promise.resolve(query) }) as never;

beforeEach(() => {
  state.session = null;
  state.pending = null;
});

describe("login page", () => {
  test("asks for the email", async () => {
    render(await LoginPage(params()));

    expect(screen.getByRole("heading", { name: "Log in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(screen.queryByRole("status")).toBeNull();
  });

  test("explains a logout after inactivity", async () => {
    render(await LoginPage(params({ expired: "1" })));

    expect(screen.getByRole("status")).toHaveTextContent("logged out after 30 minutes without activity");
  });

  test("an admin already logged in goes home", async () => {
    state.session = { ...aal1, mfa: true };

    expect(await redirectOf(() => LoginPage(params()))).toBe("/");
  });
});

describe("email code page", () => {
  test("names the email the code went to", async () => {
    state.pending = { email: "admin@glide.test", sentAt: 0 };

    render(await EmailCodePage());

    expect(screen.getByRole("heading", { name: "Check your email" })).toBeInTheDocument();
    expect(screen.getByText("admin@glide.test")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Use another email" })).toHaveAttribute("href", "/login");
    // Supabase's email codes can be up to 10 digits (the dev project sends 8).
    expect(screen.getByLabelText("Code from the email")).toHaveAttribute("maxlength", "10");
  });

  test("without a code having been sent, back to the email step", async () => {
    expect(await redirectOf(() => EmailCodePage())).toBe("/login");
  });
});

describe("authenticator pages", () => {
  test("setup and code pages need a login that passed the email code", async () => {
    expect(await redirectOf(() => AuthenticatorSetupPage())).toBe("/login");
    expect(await redirectOf(() => AuthenticatorCodePage())).toBe("/login");
  });

  test("a login that already passed MFA goes home", async () => {
    state.session = { ...aal1, mfa: true };

    expect(await redirectOf(() => AuthenticatorSetupPage())).toBe("/");
    expect(await redirectOf(() => AuthenticatorCodePage())).toBe("/");
  });

  test("setup page", async () => {
    state.session = aal1;

    render(await AuthenticatorSetupPage());

    expect(screen.getByRole("heading", { name: "Set up your authenticator app" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Set up authenticator app" })).toBeInTheDocument();
  });

  test("code page", async () => {
    state.session = aal1;

    render(await AuthenticatorCodePage());

    expect(screen.getByRole("heading", { name: "Authenticator code" })).toBeInTheDocument();
    expect(screen.getByLabelText("Code from the app")).toHaveAttribute("maxlength", "6");
    expect(screen.getByRole("button", { name: "Log in" })).toBeInTheDocument();
  });
});

test("no-access page offers another email", () => {
  render(<NoAccessPage />);

  expect(screen.getByRole("heading", { name: "No access" })).toBeInTheDocument();
  expect(screen.getByText(/isn't a Glide admin/)).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Log in with another email" })).toHaveAttribute("href", "/login");
});

test("admin home shows who is logged in and a logout button", async () => {
  render(await Home());

  expect(screen.getByRole("heading", { name: "Glide Admin" })).toBeInTheDocument();
  expect(screen.getByText("admin@glide.test")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Log out" })).toBeInTheDocument();
});
