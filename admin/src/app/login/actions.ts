"use server";

import { redirect } from "next/navigation";
import { z } from "zod";
import { ApiError } from "@/api/errors";
import { checkAdmin } from "@/auth/admin-check";
import { clearPendingLogin, clearSession, readPendingLogin, readSession, writePendingLogin, writeSession } from "@/auth/cookies";
import { type AdminSession, sessionFromTokens } from "@/auth/session";
import { supabaseAuth, SupabaseAuthError, type TotpEnrollment } from "@/auth/supabase-auth";

/** What a login form shows after its action: an error (our own words, never Supabase's or the backend's). */
export type FormState = { error?: string };

export type SetupState = FormState & { enrollment?: TotpEnrollment };

const MESSAGES = {
  invalidEmail: "Enter a valid email address.",
  cannotSend: "We can't send a code to this email address.",
  invalidEmailCode: "Enter the code from the email (only its digits).",
  invalidAppCode: "Enter the 6-digit code from your authenticator app.",
  wrongCode: "That code is wrong or has expired. Check it and try again.",
  rateLimited: "Too many attempts. Wait a few minutes and try again.",
  unavailable: "Login isn't working right now. Try again in a minute.",
  backendDown: "Glide's server isn't answering. Try again in a minute.",
} as const;

const emailSchema = z.string().trim().toLowerCase().pipe(z.email()).pipe(z.string().max(254));
const digits = (pattern: RegExp) =>
  z
    .string()
    .transform((code) => code.replace(/\s/g, ""))
    .pipe(z.string().regex(pattern));
/** Supabase's email code: its length is a project setting (6 to 10 digits; the dev project uses 8). */
const emailCodeSchema = digits(/^\d{6,10}$/);
/** Authenticator apps always show 6 digits. */
const appCodeSchema = digits(/^\d{6}$/);

/** Step 1: email → Supabase emails a login code. */
export async function sendCode(_previous: FormState, form: FormData): Promise<FormState> {
  const email = emailSchema.safeParse(form.get("email") ?? "");
  if (!email.success) return { error: MESSAGES.invalidEmail };
  try {
    await supabaseAuth().sendEmailCode(email.data);
  } catch (error) {
    // An email without a login gets the same next screen (and simply no code), so the form can't be used to find out
    // which emails have a Glide login.
    if (!(error instanceof SupabaseAuthError && error.reason === "no_such_login")) return { error: authMessage(error) };
  }
  await writePendingLogin({ email: email.data, sentAt: Date.now() });
  redirect("/login/code");
}

/** Step 2: the code from the email → a login at level aal1 → admin check → authenticator setup or code. */
export async function verifyEmailCode(_previous: FormState, form: FormData): Promise<FormState> {
  const pending = await readPendingLogin();
  if (!pending) redirect("/login");
  const code = emailCodeSchema.safeParse(form.get("code") ?? "");
  if (!code.success) return { error: MESSAGES.invalidEmailCode };
  let session: AdminSession;
  try {
    session = sessionFromTokens(await supabaseAuth().verifyEmailCode(pending.email, code.data), Date.now());
  } catch (error) {
    return { error: authMessage(error) };
  }
  await clearPendingLogin();
  await writeSession(session);
  const next = await nextStep(session);
  if (typeof next !== "string") return next;
  redirect(next);
}

/**
 * First login: `intent=start` creates an authenticator (QR code + secret); `intent=verify` checks the app's first code,
 * which finishes the setup and lifts the login to aal2.
 */
export async function setUpAuthenticator(previous: SetupState, form: FormData): Promise<SetupState> {
  const session = await loginInProgress();
  const auth = supabaseAuth();
  if (form.get("intent") === "start") {
    let factors;
    try {
      factors = await auth.factors(session.accessToken);
    } catch (error) {
      return { error: authMessage(error) };
    }
    if (factors.some((f) => f.type === "totp" && f.verified)) redirect("/login/mfa");
    try {
      // Leftovers from an earlier, unfinished setup would block a new one.
      for (const factor of factors.filter((f) => !f.verified)) await auth.unenroll(session.accessToken, factor.id);
      return { enrollment: await auth.enrollTotp(session.accessToken) };
    } catch (error) {
      return { error: authMessage(error) };
    }
  }
  const factorId = String(form.get("factorId") ?? "");
  const code = appCodeSchema.safeParse(form.get("code") ?? "");
  if (!factorId || !previous.enrollment) return { error: MESSAGES.unavailable };
  if (!code.success) return { ...previous, error: MESSAGES.invalidAppCode };
  return finishMfa(session, factorId, code.data, previous);
}

/** Later logins: the code from the authenticator app set up before. */
export async function verifyAuthenticator(_previous: FormState, form: FormData): Promise<FormState> {
  const session = await loginInProgress();
  const code = appCodeSchema.safeParse(form.get("code") ?? "");
  if (!code.success) return { error: MESSAGES.invalidAppCode };
  let factorId: string | undefined;
  try {
    factorId = (await supabaseAuth().factors(session.accessToken)).find((f) => f.type === "totp" && f.verified)?.id;
  } catch (error) {
    return { error: authMessage(error) };
  }
  if (!factorId) redirect("/login/mfa/setup");
  return finishMfa(session, factorId, code.data, {});
}

export async function logout(): Promise<void> {
  const session = await readSession();
  if (session) {
    // Best effort: the cookie goes either way, and Supabase also ends idle logins itself.
    await supabaseAuth()
      .signOut(session.accessToken)
      .catch(() => undefined);
  }
  await clearSession();
  redirect("/login");
}

async function finishMfa<S extends FormState>(session: AdminSession, factorId: string, code: string, keep: S): Promise<S> {
  let upgraded: AdminSession;
  try {
    upgraded = sessionFromTokens(await supabaseAuth().verifyTotp(session.accessToken, factorId, code), Date.now(), session);
  } catch (error) {
    return { ...keep, error: authMessage(error) };
  }
  await writeSession(upgraded);
  const next = await nextStep(upgraded);
  if (typeof next !== "string") return { ...keep, ...next };
  redirect(next);
}

/** Where a fresh login goes, decided by the backend (BE-020): home, authenticator setup/code, or "no access". */
async function nextStep(session: AdminSession): Promise<string | FormState> {
  let check;
  try {
    check = await checkAdmin(session.accessToken);
  } catch (error) {
    if (error instanceof ApiError) return { error: `${MESSAGES.backendDown}${error.requestId ? ` (request ${error.requestId})` : ""}` };
    throw error;
  }
  switch (check.kind) {
    case "admin":
      return "/";
    case "signed_out":
      await clearSession();
      return "/login";
    case "not_admin":
      // Not one of ours: end the login right away, before any authenticator is set up.
      await supabaseAuth()
        .signOut(session.accessToken)
        .catch(() => undefined);
      await clearSession();
      return "/no-access";
    case "mfa_required":
      try {
        const factors = await supabaseAuth().factors(session.accessToken);
        return factors.some((f) => f.type === "totp" && f.verified) ? "/login/mfa" : "/login/mfa/setup";
      } catch (error) {
        return { error: authMessage(error) };
      }
  }
}

/** The session of a login that has passed the email code but not yet the authenticator step. */
async function loginInProgress(): Promise<AdminSession> {
  const session = await readSession();
  if (!session) redirect("/login");
  if (session.mfa) redirect("/");
  return session;
}

function authMessage(error: unknown): string {
  if (!(error instanceof SupabaseAuthError)) return MESSAGES.unavailable;
  switch (error.reason) {
    case "invalid_code":
      return MESSAGES.wrongCode;
    case "invalid_email":
      return MESSAGES.cannotSend;
    case "rate_limited":
      return MESSAGES.rateLimited;
    case "no_such_login":
      return MESSAGES.wrongCode;
    case "session_gone":
    case "unavailable":
      return MESSAGES.unavailable;
  }
}
