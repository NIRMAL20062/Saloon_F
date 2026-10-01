# 🖥️ Web Admin Panel: To Do

Internal admin panel **for our team only** (D-002). Next.js (App Router) + TypeScript + Tailwind, pnpm. Lives in `admin/`.
Workflow and template: [README.md](README.md) · Done so far: [WEB_TASKS_COMPLETED.md](WEB_TASKS_COMPLETED.md)

> ⚠️ The admin panel must **not** be deployed on a public URL until admin login exists (Phase 1, Q-005).

## Phase 0: Walking skeleton (approved scope, D-006)

### WEB-001 · Next.js admin skeleton
- **Phase:** 0 · **Status:** 🔄 Built, waiting for the team's check · **Owner:** Claude · **Depends on:** - · **Commits:** `f347192` `e132055` `be02f86` `ac9be71`
- **Why:** an empty panel that builds and runs, wired the way every later admin feature will be
- **Scope:**
  - `admin/` created with the official Next.js generator: TypeScript, App Router, Tailwind, ESLint, pnpm
  - environment variables validated at startup; `API_BASE_URL` read **on the server only**, never sent to the browser
  - security headers (CSP, frame blocking, no-sniff, referrer policy) on every page
  - `pnpm verify` = lint + typecheck + tests + build (what CI runs)
- **Done when:**
  - [x] `pnpm dev` serves the panel on `http://localhost:3000` (verified: 200, "Glide Admin"; `pnpm start` too)
  - [x] `pnpm verify` passes (lint, typegen + tsc, 9 tests, production build)
  - [x] Tests: Vitest + Testing Library set up with one component test (`page.test.tsx`), plus `env.test.ts` (3) and `security.test.ts` (5)
  - [x] Security: headers present on every page (unit-tested + checked with curl on `next start`: per-request nonce on every script, no `X-Powered-By`); invalid env var → clear message + exit code 1
  - [x] Database: none (the panel never talks to Postgres directly, only through the backend API)

### WEB-002 · Typed API client from OpenAPI
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** WEB-001, BE-010
- **Why:** admin uses the same API contract as Android without hand-copying types (D-004)
- **Scope:**
  - TypeScript types generated from `docs/api/openapi.yaml`, plus a small typed fetch client
  - backend error envelope mapped to one error type
  - generated file checked in; CI fails if it's out of date with the spec
- **Done when:**
  - [ ] Tests: client tests for success, 503 and error envelope
  - [ ] changing the spec without regenerating fails `pnpm verify`

### WEB-003 · System status page (end-to-end proof)
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** WEB-002, BE-007
- **Why:** proves admin → backend → Postgres works before real features
- **Scope:** home page shows backend status, database status and version, fetched on the server
- **Done when:**
  - [ ] Tests: component tests for UP, DOWN and unreachable backend
  - [ ] Security: raw backend errors are never rendered to the page
  - [ ] Flow: against the local backend the page shows `UP`; stopping Postgres shows `DOWN`

### WEB-004 · End-to-end browser test (Playwright)
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** WEB-003, BE-009
- **Why:** one automated test of the full real flow: browser → admin → backend → Postgres
- **Scope:** Playwright test against `docker compose --profile full` (real backend + real Postgres, no mocks)
- **Done when:**
  - [ ] `pnpm e2e` passes locally and in CI (BE-011)
  - [ ] Flow: status page shows `UP` from the real stack

## Phase 1: Outline only (details after Q-001, Q-005 in [DECISIONS.md](../docs/DECISIONS.md))

- ⬜ **WEB-1xx** Admin login by email, invite-only; existing admins add new admins (D-013, provider Q-007)
- ⬜ **WEB-1xx** **Audit log viewer**: search who changed what, by salon / user / date (uses BE audit log). _Needs from team: nothing._

## Pre-launch: Outline only

- ⬜ **WEB-9xx** Admin panel on free hosting, reachable only after login. _Needs from team: a free hosting account (e.g. Vercel), picked together._

## Features: added by the team

<!-- Add feature tasks here using the template in README.md -->
