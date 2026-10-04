import "server-only";
import { cookies } from "next/headers";
import { getEnv } from "@/env";
import {
  ABSOLUTE_LIMIT_MS,
  type AdminSession,
  cookieOptions,
  LOGIN_COOKIE,
  LOGIN_LIMIT_MS,
  type PendingLogin,
  seal,
  SESSION_COOKIE,
  unseal,
} from "./session";

/** The admin's session from the cookie, or null. Readable in pages; writing works only in Server Actions. */
export async function readSession(): Promise<AdminSession | null> {
  return unseal<AdminSession>((await cookies()).get(SESSION_COOKIE)?.value, getEnv().ADMIN_SESSION_SECRET);
}

export async function writeSession(session: AdminSession): Promise<void> {
  const remaining = Math.max(0, session.startedAt + ABSOLUTE_LIMIT_MS - Date.now());
  const value = await seal(session, getEnv().ADMIN_SESSION_SECRET, remaining);
  (await cookies()).set(SESSION_COOKIE, value, cookieOptions(remaining));
}

export async function clearSession(): Promise<void> {
  (await cookies()).delete({ name: SESSION_COOKIE, path: "/", secure: true, sameSite: "strict", httpOnly: true });
}

export async function readPendingLogin(): Promise<PendingLogin | null> {
  return unseal<PendingLogin>((await cookies()).get(LOGIN_COOKIE)?.value, getEnv().ADMIN_SESSION_SECRET);
}

export async function writePendingLogin(login: PendingLogin): Promise<void> {
  const value = await seal(login, getEnv().ADMIN_SESSION_SECRET, LOGIN_LIMIT_MS);
  (await cookies()).set(LOGIN_COOKIE, value, cookieOptions(LOGIN_LIMIT_MS));
}

export async function clearPendingLogin(): Promise<void> {
  (await cookies()).delete({ name: LOGIN_COOKIE, path: "/", secure: true, sameSite: "strict", httpOnly: true });
}
