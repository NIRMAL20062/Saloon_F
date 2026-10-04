import "server-only";
import { createHash } from "node:crypto";
import { decodeJwt, EncryptJWT, jwtDecrypt } from "jose";

/**
 * The admin's login lives in one encrypted cookie (DF-20, DF-30): httpOnly so browser JavaScript can never read the
 * tokens, Secure, SameSite=strict, and `__Host-` so no other (sub)domain can set or read it.
 */
export const SESSION_COOKIE = "__Host-glide_admin_session";

/** Between "send code" and "enter code": which email the code went to. */
export const LOGIN_COOKIE = "__Host-glide_admin_login";

/** Logged out after this long without a page view or action (DF-30). */
export const IDLE_LIMIT_MS = 30 * 60 * 1000;

/** Logged out this long after logging in, however active (DF-30). */
export const ABSOLUTE_LIMIT_MS = 12 * 60 * 60 * 1000;

/** How long an emailed code can be typed in. */
export const LOGIN_LIMIT_MS = 10 * 60 * 1000;

export type AdminSession = {
  email: string;
  accessToken: string;
  refreshToken: string;
  /** When the Supabase access token expires, ms since epoch. */
  accessExpiresAt: number;
  /** This login passed the authenticator-app step (Supabase aal2). The backend checks the token itself. */
  mfa: boolean;
  startedAt: number;
  lastSeenAt: number;
};

export type PendingLogin = { email: string; sentAt: number };

/** The parts of a Supabase login that a session is made from. */
export type SupabaseTokens = { accessToken: string; refreshToken: string; expiresAt: number };

/** A new session (or a renewed one, keeping [previous]'s start time) from fresh Supabase tokens. */
export function sessionFromTokens(tokens: SupabaseTokens, now: number, previous?: AdminSession): AdminSession {
  const claims = decodeJwt(tokens.accessToken);
  return {
    email: typeof claims.email === "string" ? claims.email : (previous?.email ?? ""),
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    accessExpiresAt: tokens.expiresAt,
    mfa: claims.aal === "aal2",
    startedAt: previous?.startedAt ?? now,
    lastSeenAt: now,
  };
}

export type SessionState = "active" | "idle" | "expired";

export function sessionState(session: AdminSession, now: number): SessionState {
  if (now - session.startedAt >= ABSOLUTE_LIMIT_MS) return "expired";
  if (now - session.lastSeenAt >= IDLE_LIMIT_MS) return "idle";
  return "active";
}

/** Encrypts and authenticates [payload] (AES-256-GCM); it can't be read or changed without the secret. */
export async function seal(payload: object, secret: string, maxAgeMs: number): Promise<string> {
  return new EncryptJWT({ ...payload })
    .setProtectedHeader({ alg: "dir", enc: "A256GCM" })
    .setIssuedAt()
    .setExpirationTime(new Date(Date.now() + maxAgeMs))
    .encrypt(keyFrom(secret));
}

/** The payload, or null for anything forged, changed, sealed with another secret, expired or not a cookie of ours. */
export async function unseal<T>(value: string | undefined, secret: string): Promise<T | null> {
  if (!value) return null;
  try {
    const { payload } = await jwtDecrypt(value, keyFrom(secret), { keyManagementAlgorithms: ["dir"], contentEncryptionAlgorithms: ["A256GCM"] });
    // iat/exp belong to the envelope, not to the session.
    delete payload.iat;
    delete payload.exp;
    return payload as T;
  } catch {
    return null;
  }
}

/** Cookie attributes for our cookies. Secure is fine on http://localhost: browsers treat it as a secure context. */
export function cookieOptions(maxAgeMs: number) {
  return { httpOnly: true, secure: true, sameSite: "strict" as const, path: "/", maxAge: Math.floor(maxAgeMs / 1000) };
}

function keyFrom(secret: string): Uint8Array {
  // Exactly 32 bytes for AES-256, whatever length of secret the team generated.
  return new Uint8Array(createHash("sha256").update(Buffer.from(secret, "base64")).digest());
}
