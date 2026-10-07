// @vitest-environment node
import { EncryptJWT } from "jose";
import { describe, expect, test } from "vitest";
import {
  ABSOLUTE_LIMIT_MS,
  type AdminSession,
  cookieOptions,
  IDLE_LIMIT_MS,
  seal,
  sessionFromTokens,
  sessionState,
  unseal,
} from "./session";

const SECRET = Buffer.alloc(32, 1).toString("base64");
const OTHER_SECRET = Buffer.alloc(32, 2).toString("base64");

/** An unsigned token shaped like Supabase's, enough for the claims the session reads. */
function accessToken(claims: object): string {
  const part = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${part({ alg: "ES256" })}.${part(claims)}.signature`;
}

const session: AdminSession = {
  email: "admin@glide.test",
  accessToken: "access",
  refreshToken: "refresh",
  accessExpiresAt: 1_000_000,
  mfa: true,
  startedAt: 0,
  lastSeenAt: 0,
};

describe("seal / unseal", () => {
  test("round-trips the session", async () => {
    expect(await unseal(await seal(session, SECRET, 60_000), SECRET)).toEqual(session);
  });

  test("the cookie value doesn't show the tokens or email", async () => {
    const sealed = await seal(session, SECRET, 60_000);

    expect(sealed).not.toContain("admin@glide.test");
    expect(Buffer.from(sealed.split(".")[3] ?? "", "base64url").toString()).not.toContain("refresh");
  });

  test("a changed, foreign, expired or garbage value is no session", async () => {
    const sealed = await seal(session, SECRET, 60_000);
    const parts = sealed.split(".");
    parts[3] = parts[3].slice(0, -2) + (parts[3].endsWith("AA") ? "BB" : "AA");

    expect(await unseal(parts.join("."), SECRET)).toBeNull();
    expect(await unseal(sealed, OTHER_SECRET)).toBeNull();
    expect(await unseal(await seal(session, SECRET, -1_000), SECRET)).toBeNull();
    expect(await unseal("not-a-cookie", SECRET)).toBeNull();
    expect(await unseal(undefined, SECRET)).toBeNull();
  });

  test("only AES-256-GCM with our key is accepted", async () => {
    const key = new Uint8Array(16);
    const weaker = await new EncryptJWT({ ...session }).setProtectedHeader({ alg: "dir", enc: "A128GCM" }).encrypt(key);

    expect(await unseal(weaker, SECRET)).toBeNull();
  });
});

describe("sessionFromTokens", () => {
  test("reads the email and the MFA level from the access token", () => {
    const tokens = { accessToken: accessToken({ email: "a@b.in", aal: "aal2" }), refreshToken: "r", expiresAt: 5 };

    expect(sessionFromTokens(tokens, 100)).toEqual({
      email: "a@b.in",
      accessToken: tokens.accessToken,
      refreshToken: "r",
      accessExpiresAt: 5,
      mfa: true,
      startedAt: 100,
      lastSeenAt: 100,
    });
    expect(sessionFromTokens({ ...tokens, accessToken: accessToken({ email: "a@b.in", aal: "aal1" }) }, 100).mfa).toBe(false);
  });

  test("a renewed session keeps its start time, so renewing never extends the 12-hour limit", () => {
    const renewed = sessionFromTokens({ accessToken: accessToken({ aal: "aal2" }), refreshToken: "r2", expiresAt: 9 }, 500, session);

    expect(renewed.startedAt).toBe(0);
    expect(renewed.lastSeenAt).toBe(500);
    expect(renewed.email).toBe("admin@glide.test");
  });
});

describe("sessionState", () => {
  test("active, then idle after 30 minutes without activity, then expired after 12 hours", () => {
    expect(sessionState(session, IDLE_LIMIT_MS - 1)).toBe("active");
    expect(sessionState(session, IDLE_LIMIT_MS)).toBe("idle");
    expect(sessionState({ ...session, lastSeenAt: ABSOLUTE_LIMIT_MS - 1 }, ABSOLUTE_LIMIT_MS)).toBe("expired");
  });
});

test("cookies are httpOnly, Secure, SameSite=strict and site-wide", () => {
  expect(cookieOptions(60_000)).toEqual({ httpOnly: true, secure: true, sameSite: "strict", path: "/", maxAge: 60 });
});
