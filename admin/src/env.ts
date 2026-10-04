import "server-only";
import { z } from "zod";

/**
 * Server-side configuration. Read only on the server; never prefix these with NEXT_PUBLIC_
 * (that would bake them into the JavaScript sent to browsers).
 */
const schema = z.object({
  /** Backend base URL, e.g. http://localhost:8080. Called from the server only. */
  API_BASE_URL: z.url({ protocol: /^https?$/, error: "must be an http(s) URL, e.g. http://localhost:8080" }),
  /** Supabase project that admins log in to (D-016). Called from the server only (DF-20). */
  SUPABASE_URL: z.url({ protocol: /^https?$/, error: "must be the project URL, e.g. https://abcd.supabase.co" }),
  /** The project's publishable key. Public by design, but still only used on the server here. */
  SUPABASE_PUBLISHABLE_KEY: z
    .string()
    .refine((key) => !key.startsWith("sb_secret_"), "is the secret key; use the publishable key (sb_publishable_...)")
    .refine((key) => /^sb_publishable_[A-Za-z0-9_-]{8,}$/.test(key), "must be the publishable key (sb_publishable_...)"),
  /**
   * Encrypts the admin session cookie. At least 32 random bytes, base64: `openssl rand -base64 32`.
   * Changing it logs every admin out.
   */
  ADMIN_SESSION_SECRET: z
    .string()
    .refine((value) => Buffer.from(value, "base64").length >= 32, "must be at least 32 random bytes in base64 (openssl rand -base64 32)"),
});

export type Env = z.infer<typeof schema>;

/** Validates [source] and lists every problem at once, so a bad deploy shows the whole picture. */
export function parseEnv(source: Record<string, string | undefined>): Env {
  const result = schema.safeParse(source);
  if (!result.success) {
    const problems = result.error.issues.map((issue) => ` - ${issue.path.join(".")}: ${issue.message}`);
    throw new Error(`Invalid admin configuration (see admin/.env.example):\n${problems.join("\n")}`);
  }
  return result.data;
}

let cached: Env | undefined;

export function getEnv(): Env {
  cached ??= parseEnv(process.env);
  return cached;
}
