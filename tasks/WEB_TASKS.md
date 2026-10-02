# 🖥️ Web Admin Panel: To Do

Internal admin panel **for our team only** (D-002). Next.js (App Router) + TypeScript + Tailwind, pnpm. Lives in `admin/`.
Workflow and template: [README.md](README.md) · Done so far: [WEB_TASKS_COMPLETED.md](WEB_TASKS_COMPLETED.md)

> ⚠️ The admin panel must **not** be deployed on a public URL until admin login exists (Phase 1, Q-005).

## Phase 0: Walking skeleton (approved scope, D-006)

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

- ⬜ **WEB-1xx** Admin login by email code + authenticator app (DF-16) through Supabase Auth (D-016), invite-only; admins add admins (D-013)
- ⬜ **WEB-1xx** **Audit log viewer**: search who changed what, by salon / user / date (uses BE audit log). _Needs from team: nothing._

## Pre-launch: Outline only

- ⬜ **WEB-9xx** Admin panel on free hosting, reachable only after login. _Needs from team: a free hosting account (e.g. Vercel), picked together._

## Features: added by the team

<!-- Add feature tasks here using the template in README.md -->
