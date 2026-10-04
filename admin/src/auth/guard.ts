import "server-only";
import { ABSOLUTE_LIMIT_MS, type AdminSession, seal, sessionFromTokens, sessionState, type SupabaseTokens, unseal } from "./session";
import { SupabaseAuthError } from "./supabase-auth";

/** Pages anyone may open: the login steps and "no access". Every other page needs a full admin login. */
export const PUBLIC_PATHS = ["/login", "/login/code", "/login/mfa", "/login/mfa/setup", "/no-access"];

/** Refresh the Supabase access token when it has less than this left, so a page never uses an expired one. */
export const REFRESH_MARGIN_MS = 2 * 60 * 1000;

/** Activity is written to the cookie at most this often (a page view re-sends the cookie otherwise). */
export const TOUCH_EVERY_MS = 60 * 1000;

export type GuardDecision =
  | { kind: "allow"; cookie?: { value: string; maxAgeMs: number } }
  | { kind: "redirect"; location: string; clearCookie: boolean };

export type GuardDeps = { secret: string; refresh: (refreshToken: string) => Promise<SupabaseTokens> };

/** What the proxy does with one page request (DF-30). Pure apart from [deps.refresh], so every rule is unit-tested. */
export async function guard(path: string, cookie: string | undefined, now: number, deps: GuardDeps): Promise<GuardDecision> {
  if (PUBLIC_PATHS.includes(path)) return { kind: "allow" };

  const session = await unseal<AdminSession>(cookie, deps.secret);
  if (!session) {
    // A cookie we can't read (expired, changed, old secret) means the login ended; no cookie means never logged in.
    return { kind: "redirect", location: cookie ? "/login?expired=1" : "/login", clearCookie: Boolean(cookie) };
  }
  if (sessionState(session, now) !== "active") return { kind: "redirect", location: "/login?expired=1", clearCookie: true };
  if (!session.mfa) return { kind: "redirect", location: "/login/mfa", clearCookie: false };

  let next = session;
  if (session.accessExpiresAt - now < REFRESH_MARGIN_MS) {
    try {
      next = sessionFromTokens(await deps.refresh(session.refreshToken), now, session);
    } catch (error) {
      if (error instanceof SupabaseAuthError && error.reason === "session_gone") {
        return { kind: "redirect", location: "/login?expired=1", clearCookie: true };
      }
      // Supabase briefly unreachable: let the page try; the backend refuses an expired token and the page sends the
      // admin to log in again.
      return { kind: "allow" };
    }
  } else if (now - session.lastSeenAt >= TOUCH_EVERY_MS) {
    next = { ...session, lastSeenAt: now };
  } else {
    return { kind: "allow" };
  }
  const maxAgeMs = Math.max(0, next.startedAt + ABSOLUTE_LIMIT_MS - now);
  return { kind: "allow", cookie: { value: await seal(next, deps.secret, maxAgeMs), maxAgeMs } };
}
