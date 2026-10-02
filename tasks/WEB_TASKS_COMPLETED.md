# 🖥️ Web Admin Panel: Completed

Newest at the bottom. Each entry keeps the commits so anyone can `git show <hash>` to see exactly what changed.

> Groundwork the panel already relies on (done in the backend/platform files):
> [BE-007](BACKEND_TASKS_COMPLETED.md#be-007--health-readiness-endpoint) `/health` endpoint ·
> [BE-008](BACKEND_TASKS_COMPLETED.md#be-008--security-baseline--standard-error-envelope) error envelope and CORS

## Phase 0: Walking skeleton

### WEB-001 · Next.js admin skeleton
- **Completed:** 2026-10-02 · **Commits:** `f347192` `e132055` `be02f86` `ac9be71`
- **Built:** `admin/` from create-next-app 16.3.8 (TypeScript, App Router, Tailwind 4, ESLint, pnpm); `src/env.ts` (zod, server-only `API_BASE_URL`), `src/instrumentation.ts` (invalid config → clear message + exit 1), `src/proxy.ts` + `src/security/` (per-request nonce CSP, static security headers, no `X-Powered-By`), every page dynamic so the nonce applies; `pnpm verify` = lint + typegen/tsc + Vitest + build; `admin/CLAUDE.md`
- **Tests:** `page.test.tsx` (1), `env.test.ts` (3), `security.test.ts` (5) = 9
- **Verified (Claude):** `pnpm dev` and `pnpm start` serve "Glide Admin" (200); on `next start` every page script carries that request's nonce and the nonce changes per request; bad `API_BASE_URL` → exit 1 with the problem listed. Team said continue on 2026-10-02
- **Security:** CSP without `unsafe-inline` (eval only in dev), `frame-ancestors 'none'`, X-Frame-Options DENY, nosniff, no-referrer, Permissions-Policy, noindex, HSTS in production; backend URL never reaches the browser
- **Database:** none

