// @vitest-environment node
import { describe, expect, test } from "vitest";
import { buildCsp, createNonce } from "./csp";
import { securityHeaders } from "./headers";

describe("Content-Security-Policy", () => {
  test("only scripts with this request's nonce may run in production", () => {
    const csp = buildCsp("abc123", false);

    expect(csp).toContain("script-src 'self' 'nonce-abc123' 'strict-dynamic'");
    expect(csp).not.toContain("unsafe-inline");
    expect(csp).not.toContain("unsafe-eval");
    expect(csp).toContain("frame-ancestors 'none'");
    expect(csp).toContain("object-src 'none'");
    expect(csp).toContain("upgrade-insecure-requests");
  });

  test("development allows eval (React debugging) but never inline scripts", () => {
    const csp = buildCsp("abc123", true);

    expect(csp).toContain("'unsafe-eval'");
    expect(csp).not.toContain("unsafe-inline");
    expect(csp).not.toContain("upgrade-insecure-requests");
  });

  test("every request gets a different nonce", () => {
    expect(createNonce()).not.toEqual(createNonce());
  });
});

describe("security headers", () => {
  const byKey = (isProduction: boolean) =>
    Object.fromEntries(securityHeaders(isProduction).map(({ key, value }) => [key, value]));

  test("block framing, sniffing, referrers, device APIs and indexing", () => {
    const headers = byKey(false);

    expect(headers["X-Frame-Options"]).toBe("DENY");
    expect(headers["X-Content-Type-Options"]).toBe("nosniff");
    expect(headers["Referrer-Policy"]).toBe("no-referrer");
    expect(headers["Permissions-Policy"]).toContain("camera=()");
    expect(headers["X-Robots-Tag"]).toBe("noindex, nofollow");
  });

  test("HSTS only in production", () => {
    expect(byKey(false)["Strict-Transport-Security"]).toBeUndefined();
    expect(byKey(true)["Strict-Transport-Security"]).toContain("max-age=");
  });
});
