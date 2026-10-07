import "server-only";
import { redirect } from "next/navigation";
import { cache } from "react";
import { type Admin, checkAdmin } from "./admin-check";
import { hasSessionCookie, readSession } from "./cookies";

/**
 * Every admin page starts with `const admin = await requireAdmin()`. The proxy already sends logged-out visitors to
 * /login; this is the check next to the data (Next.js auth guide: never rely on the proxy or a layout alone), and the
 * backend decides who is an admin. Cached per request.
 */
export const requireAdmin = cache(async (): Promise<Admin> => {
  const session = await readSession();
  // A cookie without a usable session (idle, too old, unreadable) is a login that ended; the proxy deletes it there.
  if (!session) redirect((await hasSessionCookie()) ? "/login?expired=1" : "/login");
  if (!session.mfa) redirect("/login/mfa");
  const check = await checkAdmin(session.accessToken);
  switch (check.kind) {
    case "admin":
      return check.admin;
    case "not_admin":
      redirect("/no-access");
    case "mfa_required":
      redirect("/login/mfa");
    case "signed_out":
      redirect("/login?expired=1");
  }
});
