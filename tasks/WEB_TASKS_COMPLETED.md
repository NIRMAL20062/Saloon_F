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

## Phase 1: Admin login and salon verification

### WEB-002 · Typed API client
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-010
- **Why:** admin pages call the backend with types generated from `docs/api/openapi.yaml`, so nothing is hand-copied.
- **Done when:**
  - [x] TypeScript types generated from the spec; small typed fetch client used only on the server
  - [x] backend error envelope mapped to one error type; tests for success, error envelope, backend down
  - [x] changing the spec without regenerating fails `pnpm verify`
- **Completed:** 2026-10-04 · **Commits:** `e7c0b22` `dfa6aee` `91a81fb` `4e0f466` · PR #10
- **Built:** `openapi-typescript` 7.13.0 generates `admin/src/api/schema.d.ts` from `docs/api/openapi.yaml` (`pnpm gen:api`);
  `pnpm verify` starts with `check:api` (fails when the committed types differ from the spec). `src/api/client.ts` (server only):
  `openapi-fetch` 0.17.0 with the generated paths, 10 s timeout, `redirect: "error"`; `unwrap()` returns the typed body or
  throws `ApiError` (`src/api/errors.ts`): backend `error.code` + status + request ID, `BACKEND_UNREACHABLE` (no answer /
  timeout), `UNEXPECTED_RESPONSE` (no envelope, empty body, broken JSON). The backend's message is not kept. DF-27
- **Tests:** `client.test.ts` (10, incl. a compile-time check that calls outside the spec don't build); admin total 20
- **Verified (Claude):** against the real backend on Supabase's Postgres, `GET /health` typed `{UP, database UP}`; `GET /v1/me`
  without / with a bad token → `ApiError UNAUTHORIZED 401` with request ID; editing an enum in the spec made `check:api` fail.
  No phone test (admin-only, nothing visible). Team OK on 2026-10-04
- **Security:** `server-only`, no `NEXT_PUBLIC_` vars, no logging, backend error text never reaches pages, no redirects followed
  (tokens can't leak to another host); `pnpm audit --prod` clean
- **Database:** none
