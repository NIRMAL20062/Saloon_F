# ⚙️ Backend + Platform: To Do

Backend = API + database, shared by the Android app and the web admin panel.
Platform = repo-wide work (build, CI/CD, docs, AI tooling) that belongs to neither app alone.
Workflow and template: [README.md](README.md) · Done so far: [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md)

## Phase 0: Walking skeleton (approved scope, D-006)

### BE-009 · Backend Docker image
- **Phase:** 0 · **Status:** 🔄 In progress · **Owner:** Claude · **Depends on:** BE-008
- **Why:** CD needs one deployable artifact that runs the same on any host (Q-004 still open)
- **Scope:**
  - multi-stage `backend/Dockerfile` (JDK 21 build → JRE 21 alpine runtime), non-root user, container health check
  - `.dockerignore` so secrets and unrelated folders never enter the build context
  - `docker-compose.yml` `full` profile: backend + Postgres together (used by end-to-end tests)
  - NOT included: pushing the image anywhere (that's BE-013)
- **Done when:**
  - [ ] `docker build -f backend/Dockerfile .` succeeds from a clean checkout
  - [ ] container answers `/health` with `UP` against the compose Postgres
  - [ ] Security: runs as non-root; `.env`, keystores and `google-services.json` excluded from the context
  - [ ] Database: migrations apply on container start
  - [ ] Flow: `docker compose --profile full up` → `curl /health` → `UP`

### BE-010 · OpenAPI spec + contract test
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** BE-008
- **Why:** the web admin panel is TypeScript and can't use the Kotlin `shared` module; it generates its types from this spec (D-004)
- **Scope:**
  - `docs/api/openapi.yaml` describing `/health/live`, `/health` and the error envelope
  - backend test that validates real responses against the spec, so the spec can't silently drift
- **Done when:**
  - [ ] spec covers every current endpoint and status code (200, 503, 404, 429, 500 envelope)
  - [ ] Tests: contract test fails if a response doesn't match the spec
  - [ ] Security: n/a (no auth yet)
  - [ ] Database: none

### BE-011 · CI pipeline (GitHub Actions)
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** BE-009, APP-001, WEB-001
- **Why:** every PR must prove it builds and passes all tests before merge
- **Scope:**
  - `ci.yml` on every PR and push to `main`: ktlint, shared + backend tests (real Postgres via Testcontainers), Android lint + unit/UI tests + debug build, web lint + typecheck + tests + build, end-to-end tests
  - jobs run only for the parts a PR touched; one final `ci-ok` check to require in branch protection
- **Done when:**
  - [ ] a PR touching only `admin/` skips the Android job, and vice versa
  - [ ] a failing test anywhere blocks `ci-ok`
  - [ ] Security: workflows use least-privilege `permissions:`; third-party actions pinned
  - [ ] Flow: end-to-end job runs web → backend → Postgres

### BE-012 · Security scanning
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** BE-011
- **Scope:** gitleaks (secrets in commits), CodeQL (Kotlin + TypeScript), dependency review on PRs, Dependabot (Gradle, npm, GitHub Actions, Docker)
- **Done when:**
  - [ ] a committed fake secret fails the gitleaks check
  - [ ] CodeQL runs on PRs and weekly
  - [ ] Dependabot config covers all four ecosystems

### BE-013 · CD pipeline
- **Phase:** 0 · **Status:** ⬜ To do · **Depends on:** BE-011
- **Why:** D-008: hosting decided later, but artifacts must already be produced automatically
- **Scope:**
  - on merge to `main`: backend image → GitHub Container Registry (tagged with commit SHA)
  - Android build → Firebase App Distribution **when** Firebase secrets are added (skips cleanly until then)
  - deploy step stubbed until hosting is chosen (Q-004)
- **Done when:**
  - [ ] image appears in GHCR after a merge
  - [ ] workflow passes with no Firebase secrets configured (step skipped, not failed)
  - [ ] Security: secrets only from GitHub Actions secrets / environments; nothing printed to logs

### BE-015 · Project docs
- **Phase:** 0 · **Status:** 🔄 In progress · **Owner:** Claude · **Depends on:** - · **Progress:** docs written; final "new teammate" check after APP-001 and WEB-001 exist
- **Scope:** `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT_WORKFLOW.md` (branches, small commits, PRs, environments), `docs/TESTING.md` (incl. staging on real services with test users per role, D-008), `docs/SECURITY.md`, `docs/DATABASE.md`, `.github` PR template linking to the task's "Done when" list
- **Done when:**
  - [ ] a new teammate can set up and run all three projects from the docs alone

## Phase 1: Outline only (details after Q-001, Q-003, Q-005 in [DECISIONS.md](../docs/DECISIONS.md))

- ⬜ **BE-1xx** Multi-tenant base: `salons` table, `salon_id` scoping pattern for every table, tenant-isolation tests
- ⬜ **BE-1xx** Auth: Firebase phone OTP → backend verifies → issues JWT
- ⬜ **BE-1xx** Seed data: test users for every user type, for staging (D-008)

## Features: added by the team

<!-- Add feature tasks here using the template in README.md -->
