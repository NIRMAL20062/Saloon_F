import "server-only";
import { getEnv } from "@/env";
import type { SupabaseTokens } from "./session";

/**
 * The few Supabase Auth calls the admin login needs, made from the Next.js server only (DF-20): the browser never sees
 * a Supabase URL, key or token. Plain REST, like the Android app (DF-19), so there's no Supabase SDK to keep tokens in
 * browser storage.
 */

/** Why a call failed, in the terms the login screens need. Supabase's own messages are never kept or shown. */
export type AuthFailure =
  | "invalid_code" // wrong or expired email code / authenticator code
  | "no_such_login" // the email has no Glide login (we never create one from the login page)
  | "invalid_email"
  | "rate_limited"
  | "session_gone" // the login was signed out or expired at Supabase
  | "unavailable"; // Supabase didn't answer or failed

export class SupabaseAuthError extends Error {
  override readonly name = "SupabaseAuthError";

  constructor(
    readonly reason: AuthFailure,
    readonly status?: number,
    options?: { cause?: unknown },
  ) {
    super(`Supabase auth failed: ${reason}${status ? ` (HTTP ${status})` : ""}`, options);
  }
}

export type TotpEnrollment = {
  factorId: string;
  /** The QR code as an <img> source (base64 SVG data URI). */
  qrCode: string;
  /** The same secret as text, for typing into the app by hand. */
  secret: string;
};

export type Factor = { id: string; type: string; verified: boolean };

const TIMEOUT_MS = 10_000;

export function createSupabaseAuth(
  url: string,
  publishableKey: string,
  options: { fetch?: typeof globalThis.fetch; now?: () => number } = {},
) {
  const fetchImpl = options.fetch ?? globalThis.fetch;
  const now = options.now ?? Date.now;
  const base = `${url.replace(/\/$/, "")}/auth/v1`;

  async function call(method: string, path: string, body?: object, accessToken?: string): Promise<Record<string, unknown>> {
    let response: Response;
    try {
      response = await fetchImpl(`${base}${path}`, {
        method,
        headers: {
          apikey: publishableKey,
          "Content-Type": "application/json",
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
        cache: "no-store",
        redirect: "error",
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
    } catch (cause) {
      throw new SupabaseAuthError("unavailable", undefined, { cause });
    }
    const json = (await response.json().catch(() => ({}))) as Record<string, unknown>;
    if (!response.ok) throw new SupabaseAuthError(failureOf(response.status, json.error_code), response.status);
    return json;
  }

  function tokens(json: Record<string, unknown>): SupabaseTokens {
    const { access_token, refresh_token, expires_at, expires_in } = json;
    if (typeof access_token !== "string" || typeof refresh_token !== "string") throw new SupabaseAuthError("unavailable");
    const expiresAt =
      typeof expires_at === "number" ? expires_at * 1000 : now() + (typeof expires_in === "number" ? expires_in : 3600) * 1000;
    return { accessToken: access_token, refreshToken: refresh_token, expiresAt };
  }

  return {
    /** Emails a 6-digit code. Never creates a login: an email without one gets no code (and the screen looks the same). */
    async sendEmailCode(email: string): Promise<void> {
      await call("POST", "/otp", { email, create_user: false });
    },

    async verifyEmailCode(email: string, code: string): Promise<SupabaseTokens> {
      return tokens(await call("POST", "/verify", { type: "email", email, token: code }));
    },

    async refresh(refreshToken: string): Promise<SupabaseTokens> {
      return tokens(await call("POST", "/token?grant_type=refresh_token", { refresh_token: refreshToken }));
    },

    async factors(accessToken: string): Promise<Factor[]> {
      const user = await call("GET", "/user", undefined, accessToken);
      const list = Array.isArray(user.factors) ? (user.factors as Record<string, unknown>[]) : [];
      return list.map((f) => ({ id: String(f.id), type: String(f.factor_type), verified: f.status === "verified" }));
    },

    async enrollTotp(accessToken: string): Promise<TotpEnrollment> {
      const json = await call("POST", "/factors", { factor_type: "totp", friendly_name: "Glide Admin", issuer: "Glide Admin" }, accessToken);
      const totp = (json.totp ?? {}) as Record<string, unknown>;
      if (typeof json.id !== "string" || typeof totp.qr_code !== "string" || typeof totp.secret !== "string") {
        throw new SupabaseAuthError("unavailable");
      }
      return { factorId: json.id, qrCode: svgDataUri(totp.qr_code), secret: totp.secret };
    },

    async unenroll(accessToken: string, factorId: string): Promise<void> {
      await call("DELETE", `/factors/${encodeURIComponent(factorId)}`, undefined, accessToken);
    },

    /** Checks an authenticator code. Success returns a new login at level aal2. */
    async verifyTotp(accessToken: string, factorId: string, code: string): Promise<SupabaseTokens> {
      const id = encodeURIComponent(factorId);
      const challenge = await call("POST", `/factors/${id}/challenge`, {}, accessToken);
      if (typeof challenge.id !== "string") throw new SupabaseAuthError("unavailable");
      return tokens(await call("POST", `/factors/${id}/verify`, { challenge_id: challenge.id, code }, accessToken));
    },

    /** Ends this login at Supabase (its refresh token stops working). */
    async signOut(accessToken: string): Promise<void> {
      await call("POST", "/logout?scope=local", undefined, accessToken);
    },
  };
}

export type SupabaseAuth = ReturnType<typeof createSupabaseAuth>;

let shared: SupabaseAuth | undefined;

export function supabaseAuth(): SupabaseAuth {
  const env = getEnv();
  shared ??= createSupabaseAuth(env.SUPABASE_URL, env.SUPABASE_PUBLISHABLE_KEY);
  return shared;
}

function failureOf(status: number, errorCode: unknown): AuthFailure {
  const code = typeof errorCode === "string" ? errorCode : "";
  if (status === 429 || code.startsWith("over_")) return "rate_limited";
  if (["otp_expired", "mfa_verification_failed", "mfa_challenge_expired"].includes(code)) return "invalid_code";
  if (["otp_disabled", "user_not_found", "signup_disabled"].includes(code)) return "no_such_login";
  if (["email_address_invalid", "email_address_not_authorized"].includes(code)) return "invalid_email";
  if (status === 401 || code.startsWith("refresh_token_") || ["session_not_found", "session_expired", "bad_jwt"].includes(code)) {
    return "session_gone";
  }
  return "unavailable";
}

/** Supabase returns `data:image/svg+xml;utf-8,<svg…>`; base64 makes it a safe <img> source whatever the SVG contains. */
function svgDataUri(qrCode: string): string {
  if (qrCode.startsWith("data:image/svg+xml;base64,")) return qrCode;
  const svg = qrCode.slice(qrCode.indexOf(",") + 1);
  return `data:image/svg+xml;base64,${Buffer.from(svg, "utf8").toString("base64")}`;
}
