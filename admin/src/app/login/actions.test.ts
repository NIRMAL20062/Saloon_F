// @vitest-environment node
import { beforeEach, describe, expect, test, vi } from "vitest";
import { ApiError } from "@/api/errors";
import { type AdminSession, LOGIN_COOKIE, seal, SESSION_COOKIE, unseal } from "@/auth/session";
import { SupabaseAuthError } from "@/auth/supabase-auth";
import { accessToken, createCookieJar, fakeRedirect, redirectOf, TEST_ENV } from "@/test/next-fakes";

const cookies = createCookieJar();
const supabase = {
  sendEmailCode: vi.fn(),
  verifyEmailCode: vi.fn(),
  factors: vi.fn(),
  enrollTotp: vi.fn(),
  unenroll: vi.fn(),
  verifyTotp: vi.fn(),
  signOut: vi.fn(),
};
const checkAdmin = vi.fn();

vi.mock("next/headers", () => ({ cookies: async () => cookies.store }));
vi.mock("next/navigation", () => ({ redirect: (url: string) => fakeRedirect(url) }));
vi.mock("@/env", () => ({ getEnv: () => TEST_ENV }));
vi.mock("@/auth/admin-check", () => ({ checkAdmin: (token: string) => checkAdmin(token) }));
vi.mock("@/auth/supabase-auth", async (original) => ({
  ...(await original<typeof import("@/auth/supabase-auth")>()),
  supabaseAuth: () => supabase,
}));

const { continueLogin, logout, sendCode, setUpAuthenticator, verifyAuthenticator, verifyEmailCode } = await import("./actions");

const SECRET = TEST_ENV.ADMIN_SESSION_SECRET;
const AAL1 = accessToken({ email: "admin@glide.test", aal: "aal1" });
const AAL2 = accessToken({ email: "admin@glide.test", aal: "aal2" });
const tokens = (access: string) => ({ accessToken: access, refreshToken: "rt", expiresAt: Date.now() + 3_600_000 });

function form(fields: Record<string, string>) {
  const data = new FormData();
  Object.entries(fields).forEach(([k, v]) => data.set(k, v));
  return data;
}

async function session(): Promise<AdminSession | null> {
  return unseal<AdminSession>(cookies.jar.get(SESSION_COOKIE)?.value, SECRET);
}

async function loggedInWithEmailCode() {
  const now = Date.now();
  const value = await seal(
    { email: "admin@glide.test", accessToken: AAL1, refreshToken: "rt", accessExpiresAt: now + 3_600_000, mfa: false, startedAt: now, lastSeenAt: now },
    SECRET,
    60_000,
  );
  cookies.jar.set(SESSION_COOKIE, { value });
}

async function codeSentTo(email: string) {
  cookies.jar.set(LOGIN_COOKIE, { value: await seal({ email, sentAt: Date.now() }, SECRET, 60_000) });
}

beforeEach(() => {
  cookies.jar.clear();
  vi.clearAllMocks();
  supabase.signOut.mockResolvedValue(undefined);
  supabase.unenroll.mockResolvedValue(undefined);
});

describe("sendCode", () => {
  test("a bad email shows an error and sends nothing", async () => {
    expect(await sendCode({}, form({ email: "not-an-email" }))).toEqual({ error: "Enter a valid email address." });
    expect(supabase.sendEmailCode).not.toHaveBeenCalled();
  });

  test("a good email gets a code; the email is remembered (encrypted) for the next step", async () => {
    supabase.sendEmailCode.mockResolvedValue(undefined);

    expect(await redirectOf(() => sendCode({}, form({ email: "  Admin@Glide.TEST " })))).toBe("/login/code");
    expect(supabase.sendEmailCode).toHaveBeenCalledWith("admin@glide.test");
    const login = cookies.jar.get(LOGIN_COOKIE)!;
    expect(login.value).not.toContain("admin@glide.test");
    expect(login.options).toMatchObject({ httpOnly: true, secure: true, sameSite: "strict" });
  });

  test("an email without a login looks exactly the same (no way to probe which emails exist)", async () => {
    supabase.sendEmailCode.mockRejectedValue(new SupabaseAuthError("no_such_login", 422));

    expect(await redirectOf(() => sendCode({}, form({ email: "stranger@example.com" })))).toBe("/login/code");
  });

  test.each([
    ["rate_limited", "Too many attempts. Wait a few minutes and try again."],
    ["invalid_email", "We can't send a code to this email address."],
    ["unavailable", "Login isn't working right now. Try again in a minute."],
  ] as const)("Supabase %s shows our own message", async (reason, message) => {
    supabase.sendEmailCode.mockRejectedValue(new SupabaseAuthError(reason));

    expect(await sendCode({}, form({ email: "a@b.in" }))).toEqual({ error: message });
  });
});

describe("verifyEmailCode", () => {
  test("without a code having been sent, it goes back to the email step", async () => {
    expect(await redirectOf(() => verifyEmailCode({}, form({ code: "123456" })))).toBe("/login");
  });

  test("a code that isn't 6 digits is refused before asking Supabase", async () => {
    await codeSentTo("admin@glide.test");

    expect(await verifyEmailCode({}, form({ code: "12a" }))).toEqual({ error: "Enter the code from the email (only its digits)." });
    expect(supabase.verifyEmailCode).not.toHaveBeenCalled();
  });

  test("email codes of 6 to 10 digits reach Supabase intact (the length is a Supabase setting; dev uses 8)", async () => {
    await codeSentTo("admin@glide.test");
    supabase.verifyEmailCode.mockRejectedValue(new SupabaseAuthError("invalid_code", 403));

    for (const code of ["123456", "1234 5678", "1234567890"]) await verifyEmailCode({}, form({ code }));

    expect(supabase.verifyEmailCode.mock.calls.map((call) => call[1])).toEqual(["123456", "12345678", "1234567890"]);
    expect(await verifyEmailCode({}, form({ code: "12345678901" }))).toEqual({ error: "Enter the code from the email (only its digits)." });
    expect(supabase.verifyEmailCode).toHaveBeenCalledTimes(3);
  });

  test("a wrong or expired code shows an error and logs nobody in", async () => {
    await codeSentTo("admin@glide.test");
    supabase.verifyEmailCode.mockRejectedValue(new SupabaseAuthError("invalid_code", 403));

    expect(await verifyEmailCode({}, form({ code: "000000" }))).toEqual({ error: "That code is wrong or has expired. Check it and try again." });
    expect(cookies.jar.has(SESSION_COOKIE)).toBe(false);
  });

  test("first login of an admin: session saved (httpOnly cookie), then authenticator setup", async () => {
    await codeSentTo("admin@glide.test");
    supabase.verifyEmailCode.mockResolvedValue(tokens(AAL1));
    checkAdmin.mockResolvedValue({ kind: "mfa_required" });
    supabase.factors.mockResolvedValue([]);

    expect(await redirectOf(() => verifyEmailCode({}, form({ code: "123 456" })))).toBe("/login/mfa/setup");
    expect(supabase.verifyEmailCode).toHaveBeenCalledWith("admin@glide.test", "123456");
    expect(await session()).toMatchObject({ email: "admin@glide.test", mfa: false });
    expect(cookies.jar.get(SESSION_COOKIE)!.options).toMatchObject({ httpOnly: true, secure: true, sameSite: "strict", path: "/" });
    expect(cookies.jar.has(LOGIN_COOKIE)).toBe(false);
  });

  test("an admin who set up the app before goes to the authenticator code", async () => {
    await codeSentTo("admin@glide.test");
    supabase.verifyEmailCode.mockResolvedValue(tokens(AAL1));
    checkAdmin.mockResolvedValue({ kind: "mfa_required" });
    supabase.factors.mockResolvedValue([{ id: "f1", type: "totp", verified: true }]);

    expect(await redirectOf(() => verifyEmailCode({}, form({ code: "123456" })))).toBe("/login/mfa");
  });

  test("a login that isn't an admin is signed out at once and sees 'no access'", async () => {
    await codeSentTo("someone@example.com");
    supabase.verifyEmailCode.mockResolvedValue(tokens(AAL1));
    checkAdmin.mockResolvedValue({ kind: "not_admin" });

    expect(await redirectOf(() => verifyEmailCode({}, form({ code: "123456" })))).toBe("/no-access");
    expect(supabase.signOut).toHaveBeenCalledWith(AAL1);
    expect(cookies.jar.has(SESSION_COOKIE)).toBe(false);
    expect(supabase.enrollTotp).not.toHaveBeenCalled();
  });

  test("backend down after a good code: the login is kept and a 'Try again' page takes over (the code is used up)", async () => {
    await codeSentTo("admin@glide.test");
    supabase.verifyEmailCode.mockResolvedValue(tokens(AAL1));
    checkAdmin.mockRejectedValue(new ApiError("BACKEND_UNREACHABLE", undefined, "req-9"));

    expect(await redirectOf(() => verifyEmailCode({}, form({ code: "123456" })))).toBe("/login/continue");
    expect(await session()).toMatchObject({ email: "admin@glide.test", mfa: false });
  });
});

describe("setUpAuthenticator", () => {
  const enrollment = { factorId: "f-new", qrCode: "data:image/svg+xml;base64,PHN2Zy8+", secret: "SECRET" };

  test("needs a login that passed the email code", async () => {
    expect(await redirectOf(() => setUpAuthenticator({}, form({ intent: "start" })))).toBe("/login");
  });

  test("start removes an unfinished earlier setup and returns the QR code and key", async () => {
    await loggedInWithEmailCode();
    supabase.factors.mockResolvedValue([{ id: "old", type: "totp", verified: false }]);
    supabase.enrollTotp.mockResolvedValue(enrollment);

    expect(await setUpAuthenticator({}, form({ intent: "start" }))).toEqual({ enrollment });
    expect(supabase.unenroll).toHaveBeenCalledWith(AAL1, "old");
  });

  test("an admin who already has an authenticator is sent to the code step instead", async () => {
    await loggedInWithEmailCode();
    supabase.factors.mockResolvedValue([{ id: "f1", type: "totp", verified: true }]);

    expect(await redirectOf(() => setUpAuthenticator({}, form({ intent: "start" })))).toBe("/login/mfa");
    expect(supabase.enrollTotp).not.toHaveBeenCalled();
  });

  test("a wrong first code keeps the QR code on screen with an error", async () => {
    await loggedInWithEmailCode();
    supabase.verifyTotp.mockRejectedValue(new SupabaseAuthError("invalid_code", 422));

    expect(await setUpAuthenticator({ enrollment }, form({ intent: "verify", factorId: "f-new", code: "111111" }))).toEqual({
      enrollment,
      error: "That code is wrong or has expired. Check it and try again.",
    });
  });

  test("the right first code finishes setup, upgrades the session to MFA and opens the admin home", async () => {
    await loggedInWithEmailCode();
    supabase.verifyTotp.mockResolvedValue(tokens(AAL2));
    checkAdmin.mockResolvedValue({ kind: "admin", admin: { id: "u", email: "admin@glide.test", status: "ACTIVE" } });

    expect(await redirectOf(() => setUpAuthenticator({ enrollment }, form({ intent: "verify", factorId: "f-new", code: "222222" })))).toBe("/");
    expect(supabase.verifyTotp).toHaveBeenCalledWith(AAL1, "f-new", "222222");
    expect(await session()).toMatchObject({ mfa: true, accessToken: AAL2 });
  });
});

describe("verifyAuthenticator", () => {
  test("the app's code completes the login", async () => {
    await loggedInWithEmailCode();
    supabase.factors.mockResolvedValue([{ id: "f1", type: "totp", verified: true }]);
    supabase.verifyTotp.mockResolvedValue(tokens(AAL2));
    checkAdmin.mockResolvedValue({ kind: "admin", admin: { id: "u", email: "admin@glide.test", status: "ACTIVE" } });

    expect(await redirectOf(() => verifyAuthenticator({}, form({ code: "333333" })))).toBe("/");
    expect(supabase.verifyTotp).toHaveBeenCalledWith(AAL1, "f1", "333333");
  });

  test("authenticator codes are exactly 6 digits", async () => {
    await loggedInWithEmailCode();

    expect(await verifyAuthenticator({}, form({ code: "12345678" }))).toEqual({ error: "Enter the 6-digit code from your authenticator app." });
    expect(supabase.verifyTotp).not.toHaveBeenCalled();
  });

  test("a wrong code is an error; no authenticator yet goes to setup", async () => {
    await loggedInWithEmailCode();
    supabase.factors.mockResolvedValue([{ id: "f1", type: "totp", verified: true }]);
    supabase.verifyTotp.mockRejectedValue(new SupabaseAuthError("invalid_code"));
    expect(await verifyAuthenticator({}, form({ code: "333333" }))).toEqual({ error: "That code is wrong or has expired. Check it and try again." });

    supabase.factors.mockResolvedValue([]);
    expect(await redirectOf(() => verifyAuthenticator({}, form({ code: "333333" })))).toBe("/login/mfa/setup");
  });
});

describe("continueLogin", () => {
  test("still down: our message with the request ID", async () => {
    await loggedInWithEmailCode();
    checkAdmin.mockRejectedValue(new ApiError("BACKEND_UNREACHABLE", undefined, "req-9"));

    expect(await continueLogin()).toEqual({ error: "Glide's server isn't answering. Try again in a minute. (request req-9)" });
  });

  test("back up: goes on to the right step", async () => {
    await loggedInWithEmailCode();
    checkAdmin.mockResolvedValue({ kind: "mfa_required" });
    supabase.factors.mockResolvedValue([]);

    expect(await redirectOf(() => continueLogin())).toBe("/login/mfa/setup");
  });

  test("without a login, back to the start", async () => {
    expect(await redirectOf(() => continueLogin())).toBe("/login");
  });
});

test("backend down right after the authenticator step also lands on 'Try again'", async () => {
  await loggedInWithEmailCode();
  supabase.factors.mockResolvedValue([{ id: "f1", type: "totp", verified: true }]);
  supabase.verifyTotp.mockResolvedValue(tokens(AAL2));
  checkAdmin.mockRejectedValue(new ApiError("BACKEND_UNREACHABLE", undefined, undefined));

  expect(await redirectOf(() => verifyAuthenticator({}, form({ code: "333333" })))).toBe("/login/continue");
  expect(await session()).toMatchObject({ mfa: true });
});

test("logout ends the login at Supabase, deletes the cookie and shows the login page", async () => {
  await loggedInWithEmailCode();

  expect(await redirectOf(() => logout())).toBe("/login");
  expect(supabase.signOut).toHaveBeenCalledWith(AAL1);
  expect(cookies.jar.has(SESSION_COOKIE)).toBe(false);
});

test("logout still works when Supabase doesn't answer", async () => {
  await loggedInWithEmailCode();
  supabase.signOut.mockRejectedValue(new SupabaseAuthError("unavailable"));

  expect(await redirectOf(() => logout())).toBe("/login");
  expect(cookies.jar.has(SESSION_COOKIE)).toBe(false);
});
