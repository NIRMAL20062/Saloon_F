/**
 * Content Security Policy for the admin panel, following the Next.js 16 nonce guide
 * (node_modules/next/dist/docs/01-app/02-guides/content-security-policy.md).
 * Only scripts carrying this request's nonce may run, so injected scripts (XSS) are blocked.
 */
export function buildCsp(nonce: string, isDev: boolean): string {
  const directives = [
    "default-src 'self'",
    // React needs eval in development only, for better error stacks.
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ""}`,
    `style-src 'self' 'nonce-${nonce}'`,
    "img-src 'self' blob: data:",
    "font-src 'self'",
    // The browser talks only to this panel; the panel's server talks to the backend.
    "connect-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    // Not in development: it would rewrite http://localhost assets to https and break the page.
    ...(isDev ? [] : ["upgrade-insecure-requests"]),
  ];
  return directives.join("; ");
}

export function createNonce(): string {
  return Buffer.from(crypto.randomUUID()).toString("base64");
}
