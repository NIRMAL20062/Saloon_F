@AGENTS.md

# admin/: Glide internal admin panel

Next.js 16 (App Router) · TypeScript · Tailwind 4 · pnpm. **For our team only** (D-002): not salon-facing, not public.
Root rules: [../CLAUDE.md](../CLAUDE.md). Tasks: [../tasks/WEB_TASKS.md](../tasks/WEB_TASKS.md).

## Layout

```
src/app/            routes (App Router). Every page is rendered per request (see layout.tsx: nonce CSP).
src/env.ts          server-only config, validated with zod; the server refuses to start if it's invalid
src/instrumentation.ts  runs env validation once at startup
src/proxy.ts        per-request nonce Content-Security-Policy (Next 16 "proxy" = old "middleware")
src/security/       CSP builder + static security headers (used by proxy.ts and next.config.ts)
src/test/           Vitest setup
```

## Rules

- **Read the bundled docs first** (`node_modules/next/dist/docs/`): this Next.js version differs from older training data.
- **The browser never talks to the backend directly.** Server Components / Route Handlers call the backend with
  `API_BASE_URL` from `getEnv()`. Never create `NEXT_PUBLIC_` variables for URLs, keys or anything sensitive.
- API types come from `docs/api/openapi.yaml` (generated, WEB-002). Never hand-write a backend request/response type.
- Never render raw backend error text. Show our own message plus the request ID.
- No inline `<script>` or `style=` that bypasses the nonce CSP. No `dangerouslySetInnerHTML`.
- Not deployable until admin login exists (Phase 1, D-013).
- New env var: add it to the zod schema in `src/env.ts`, to `.env.example`, and to the tests.

## Tests

- Vitest + Testing Library (`*.test.tsx` next to the code). Node-only tests start with `// @vitest-environment node`.
- Playwright end-to-end tests against the real backend arrive with WEB-004.

## Commands

```bash
cp .env.example .env.local   # first time
pnpm install
pnpm dev                     # http://localhost:3000
pnpm verify                  # lint + typecheck + tests + production build (what CI runs)
```
