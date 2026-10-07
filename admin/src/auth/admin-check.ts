import "server-only";
import { backend, unwrap } from "@/api/client";
import { ApiError } from "@/api/errors";
import type { components } from "@/api/schema";

export type Admin = components["schemas"]["AdminResponse"];

/** What the backend says about this login (BE-020). */
export type AdminCheck =
  | { kind: "admin"; admin: Admin }
  | { kind: "not_admin" }
  | { kind: "mfa_required" }
  | { kind: "signed_out" };

/** Asks the backend whether [accessToken]'s login is an admin with MFA done. Other failures throw [ApiError]. */
export async function checkAdmin(accessToken: string): Promise<AdminCheck> {
  try {
    const admin = await unwrap(backend().GET("/v1/admin/me", { headers: { Authorization: `Bearer ${accessToken}` } }));
    return { kind: "admin", admin };
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.code === "NOT_ADMIN") return { kind: "not_admin" };
      if (error.code === "MFA_REQUIRED") return { kind: "mfa_required" };
      if (error.code === "UNAUTHORIZED") return { kind: "signed_out" };
    }
    throw error;
  }
}
