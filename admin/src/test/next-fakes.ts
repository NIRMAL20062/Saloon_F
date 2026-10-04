/** Stand-ins for Next.js request APIs in unit tests: an in-memory cookie jar and a redirect() that throws. */

export type CookieRecord = { value: string; options?: Record<string, unknown> };

export function createCookieJar() {
  const jar = new Map<string, CookieRecord>();
  const store = {
    get: (name: string) => (jar.has(name) ? { name, value: jar.get(name)!.value } : undefined),
    set: (name: string, value: string, options?: Record<string, unknown>) => {
      jar.set(name, { value, options });
    },
    delete: (arg: string | { name: string }) => {
      jar.delete(typeof arg === "string" ? arg : arg.name);
    },
  };
  return { jar, store };
}

export class RedirectSignal extends Error {
  constructor(readonly url: string) {
    super(`redirect to ${url}`);
  }
}

export function fakeRedirect(url: string): never {
  throw new RedirectSignal(url);
}

/** Runs [run] and returns where it redirected to (fails if it didn't). */
export async function redirectOf(run: () => Promise<unknown>): Promise<string> {
  try {
    await run();
  } catch (error) {
    if (error instanceof RedirectSignal) return error.url;
    throw error;
  }
  throw new Error("expected a redirect");
}

/** An unsigned token shaped like a Supabase access token (the admin server only reads its claims). */
export function accessToken(claims: { email?: string; aal?: string }): string {
  const part = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${part({ alg: "ES256" })}.${part(claims)}.signature`;
}

export const TEST_ENV = {
  API_BASE_URL: "http://backend.test",
  SUPABASE_URL: "https://abcd.supabase.co",
  SUPABASE_PUBLISHABLE_KEY: "sb_publishable_test_key_123",
  ADMIN_SESSION_SECRET: Buffer.alloc(32, 9).toString("base64"),
};
