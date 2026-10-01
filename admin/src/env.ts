import "server-only";
import { z } from "zod";

/**
 * Server-side configuration. Read only on the server; never prefix these with NEXT_PUBLIC_
 * (that would bake them into the JavaScript sent to browsers).
 */
const schema = z.object({
  /** Backend base URL, e.g. http://localhost:8080. Called from the server only. */
  API_BASE_URL: z.url({ protocol: /^https?$/, error: "must be an http(s) URL, e.g. http://localhost:8080" }),
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
