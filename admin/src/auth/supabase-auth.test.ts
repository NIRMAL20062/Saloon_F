// @vitest-environment node
import { describe, expect, test } from "vitest";
import { createSupabaseAuth, SupabaseAuthError } from "./supabase-auth";

const URL = "https://abcd.supabase.co";
const KEY = "sb_publishable_test_key_123";

type Seen = { method: string; url: string; headers: Headers; body: unknown };

/** A stand-in Supabase answering every call with [answer]; [seen] keeps the requests. */
function fake(answer: (request: Seen) => { status?: number; body?: unknown }) {
  const seen: Seen[] = [];
  const auth = createSupabaseAuth(URL, KEY, {
    now: () => 1_000,
    fetch: async (input, init) => {
      const request: Seen = {
        method: init?.method ?? "GET",
        url: String(input),
        headers: new Headers(init?.headers),
        body: init?.body ? JSON.parse(String(init.body)) : undefined,
      };
      seen.push(request);
      const { status = 200, body = {} } = answer(request);
      return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
    },
  });
  return { auth, seen };
}

async function failure(call: Promise<unknown>) {
  const error = await call.then(
    () => undefined,
    (e: unknown) => e,
  );
  expect(error).toBeInstanceOf(SupabaseAuthError);
  return error as SupabaseAuthError;
}

const SESSION = { access_token: "at", refresh_token: "rt", expires_at: 2_000 };

describe("Supabase auth client", () => {
  test("sends the email code without ever creating a login, with only the publishable key", async () => {
    const { auth, seen } = fake(() => ({}));

    await auth.sendEmailCode("a@b.in");

    expect(seen[0].method).toBe("POST");
    expect(seen[0].url).toBe(`${URL}/auth/v1/otp`);
    expect(seen[0].body).toEqual({ email: "a@b.in", create_user: false });
    expect(seen[0].headers.get("apikey")).toBe(KEY);
    expect(seen[0].headers.get("Authorization")).toBeNull();
  });

  test("verifies the email code and returns the tokens", async () => {
    const { auth, seen } = fake(() => ({ body: SESSION }));

    expect(await auth.verifyEmailCode("a@b.in", "123456")).toEqual({ accessToken: "at", refreshToken: "rt", expiresAt: 2_000_000 });
    expect(seen[0].body).toEqual({ type: "email", email: "a@b.in", token: "123456" });
  });

  test("refreshes with the refresh token; a missing expires_at falls back to expires_in", async () => {
    const { auth, seen } = fake(() => ({ body: { access_token: "a2", refresh_token: "r2", expires_in: 60 } }));

    expect(await auth.refresh("rt")).toEqual({ accessToken: "a2", refreshToken: "r2", expiresAt: 61_000 });
    expect(seen[0].url).toBe(`${URL}/auth/v1/token?grant_type=refresh_token`);
    expect(seen[0].body).toEqual({ refresh_token: "rt" });
  });

  test("lists the login's factors with its own token", async () => {
    const { auth, seen } = fake(() => ({
      body: { factors: [{ id: "f1", factor_type: "totp", status: "verified" }, { id: "f2", factor_type: "totp", status: "unverified" }] },
    }));

    expect(await auth.factors("at")).toEqual([
      { id: "f1", type: "totp", verified: true },
      { id: "f2", type: "totp", verified: false },
    ]);
    expect(seen[0].headers.get("Authorization")).toBe("Bearer at");
  });

  test("a login without factors has none", async () => {
    expect(await fake(() => ({ body: { id: "u" } })).auth.factors("at")).toEqual([]);
  });

  test("enrolling an authenticator returns the QR code as a base64 image and the secret", async () => {
    const svg = '<svg xmlns="http://www.w3.org/2000/svg"><path fill="#000" d="M0 0"/></svg>';
    const { auth, seen } = fake(() => ({ body: { id: "f1", type: "totp", totp: { qr_code: `data:image/svg+xml;utf-8,${svg}`, secret: "ABC", uri: "otpauth://x" } } }));

    const enrollment = await auth.enrollTotp("at");

    expect(enrollment.factorId).toBe("f1");
    expect(enrollment.secret).toBe("ABC");
    expect(enrollment.qrCode).toBe(`data:image/svg+xml;base64,${Buffer.from(svg).toString("base64")}`);
    expect(seen[0].body).toMatchObject({ factor_type: "totp" });
  });

  test("an authenticator code is checked with a challenge, then verify", async () => {
    const { auth, seen } = fake((request) => ({ body: request.url.endsWith("/challenge") ? { id: "ch1" } : SESSION }));

    expect((await auth.verifyTotp("at", "f/1", "654321")).accessToken).toBe("at");
    expect(seen.map((r) => r.url)).toEqual([`${URL}/auth/v1/factors/f%2F1/challenge`, `${URL}/auth/v1/factors/f%2F1/verify`]);
    expect(seen[1].body).toEqual({ challenge_id: "ch1", code: "654321" });
  });

  test("unenroll and sign out call Supabase with the login's token", async () => {
    const { auth, seen } = fake(() => ({}));

    await auth.unenroll("at", "f1");
    await auth.signOut("at");

    expect(seen.map((r) => `${r.method} ${r.url}`)).toEqual([`DELETE ${URL}/auth/v1/factors/f1`, `POST ${URL}/auth/v1/logout?scope=local`]);
  });

  test.each([
    [403, "otp_expired", "invalid_code"],
    [422, "mfa_verification_failed", "invalid_code"],
    [422, "otp_disabled", "no_such_login"],
    [400, "email_address_invalid", "invalid_email"],
    [429, "over_email_send_rate_limit", "rate_limited"],
    [400, "refresh_token_not_found", "session_gone"],
    [401, "bad_jwt", "session_gone"],
    [500, "unexpected_failure", "unavailable"],
  ])("HTTP %i %s is %s, without Supabase's message", async (status, code, reason) => {
    const { auth } = fake(() => ({ status, body: { code: status, error_code: code, msg: "secret detail about a@b.in" } }));

    const error = await failure(auth.verifyEmailCode("a@b.in", "123456"));

    expect(error.reason).toBe(reason);
    expect(error.message).not.toContain("a@b.in");
  });

  test("Supabase unreachable is unavailable", async () => {
    const auth = createSupabaseAuth(URL, KEY, { fetch: () => Promise.reject(new TypeError("fetch failed")) });

    expect((await failure(auth.sendEmailCode("a@b.in"))).reason).toBe("unavailable");
  });

  test("never follows redirects", async () => {
    let redirect: RequestRedirect | undefined;
    const auth = createSupabaseAuth(URL, KEY, {
      fetch: async (_input, init) => {
        redirect = init?.redirect;
        return new Response("{}");
      },
    });

    await auth.sendEmailCode("a@b.in");

    expect(redirect).toBe("error");
  });
});
