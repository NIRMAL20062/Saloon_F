// @vitest-environment node
import { describe, expect, test } from "vitest";
import { parseEnv } from "./env";

describe("parseEnv", () => {
  test("accepts a valid configuration", () => {
    expect(parseEnv({ API_BASE_URL: "http://localhost:8080" })).toEqual({ API_BASE_URL: "http://localhost:8080" });
  });

  test("fails fast with a clear message when API_BASE_URL is missing", () => {
    expect(() => parseEnv({})).toThrow(/Invalid admin configuration[\s\S]*API_BASE_URL/);
  });

  test("rejects a value that is not an http(s) URL", () => {
    expect(() => parseEnv({ API_BASE_URL: "localhost:8080" })).toThrow(/API_BASE_URL: must be an http\(s\) URL/);
    expect(() => parseEnv({ API_BASE_URL: "ftp://example.com" })).toThrow(/API_BASE_URL/);
  });
});
