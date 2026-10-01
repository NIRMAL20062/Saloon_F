/** Static security headers sent with every response (the per-request CSP is added in proxy.ts). */
export function securityHeaders(isProduction: boolean): { key: string; value: string }[] {
  return [
    { key: "X-Frame-Options", value: "DENY" },
    { key: "X-Content-Type-Options", value: "nosniff" },
    { key: "Referrer-Policy", value: "no-referrer" },
    { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(), payment=()" },
    // Internal tool: keep it out of search engines even if a URL leaks.
    { key: "X-Robots-Tag", value: "noindex, nofollow" },
    ...(isProduction ? [{ key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains" }] : []),
  ];
}
