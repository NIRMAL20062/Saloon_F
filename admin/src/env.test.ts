// @vitest-environment node
import { describe, expect, test } from "vitest";
import { parseEnv } from "./env";

const valid = {
  API_BASE_URL: "http://localhost:8080",
  SUPABASE_URL: "https://abcd.supabase.co",
  SUPABASE_PUBLISHABLE_KEY: "sb_publishable_test_key_123",
  ADMIN_SESSION_SECRET: Buffer.alloc(32, 7).toString("base64"),
};

describe("parseEnv", () => {
  test("accepts a valid configuration", () => {
    expect(parseEnv(valid)).toEqual(valid);
  });

  test("fails fast and lists every missing value at once", () => {
    expect(() => parseEnv({})).toThrow(
      /Invalid admin configuration[\s\S]*API_BASE_URL[\s\S]*SUPABASE_URL[\s\S]*SUPABASE_PUBLISHABLE_KEY[\s\S]*ADMIN_SESSION_SECRET/,
    );
  });

  test("rejects a value that is not an http(s) URL", () => {
    expect(() => parseEnv({ ...valid, API_BASE_URL: "localhost:8080" })).toThrow(/API_BASE_URL: must be an http\(s\) URL/);
    expect(() => parseEnv({ ...valid, API_BASE_URL: "ftp://example.com" })).toThrow(/API_BASE_URL/);
    expect(() => parseEnv({ ...valid, SUPABASE_URL: "abcd.supabase.co" })).toThrow(/SUPABASE_URL/);
  });

  test("refuses the Supabase secret key in place of the publishable key, without echoing it", () => {
    const run = () => parseEnv({ ...valid, SUPABASE_PUBLISHABLE_KEY: "sb_secret_do_not_print_me" });

    expect(run).toThrow(/SUPABASE_PUBLISHABLE_KEY: is the secret key/);
    expect(run).not.toThrow(/do_not_print_me/);
  });

  test("refuses a session secret shorter than 32 bytes", () => {
    expect(() => parseEnv({ ...valid, ADMIN_SESSION_SECRET: Buffer.alloc(16).toString("base64") })).toThrow(
      /ADMIN_SESSION_SECRET: must be at least 32 random bytes/,
    );
  });
});
