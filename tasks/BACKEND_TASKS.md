# ⚙️ Backend + Platform: To Do

Backend = API + database, shared by the Android app and the web admin panel.
Platform = repo-wide work (build, CI/CD, docs, AI tooling) that belongs to neither app alone.
Workflow and template: [README.md](README.md) · Done so far: [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md)

## Phase 0: Walking skeleton (approved scope, D-006)

### BE-011 · CI pipeline (GitHub Actions)
- **Phase:** 0 · **Status:** 🔄 Built and green, waiting for the team's OK · **Owner:** Claude · **Depends on:** BE-009, APP-001, WEB-001 · **Commits:** `e8f3d5f` `2292889` (fix)
- **Why:** every PR must prove it builds and passes all tests before merge
- **Scope:**
  - `ci.yml` on every PR and push to `main`: ktlint, shared + backend tests (real Postgres via Testcontainers), Android lint + unit/UI tests + debug build, web lint + typecheck + tests + build, end-to-end tests
  - jobs run only for the parts a PR touched; one final `ci-ok` check to require in branch protection
- **Done when:**
  - [x] first run on `main` is green: run 36980498414 (2026-10-02), all 6 jobs ✅, ~10 min total (repo is public now, so Claude reads results via the GitHub API)
  - [ ] a PR touching only `admin/` skips the Android job, and vice versa (`dorny/paths-filter`; confirm on the first real PR)
  - [x] a failing job blocks `ci-ok`: seen for real in run 36977637474, where a config bug failed "Detect changed areas" and `ci-ok` went red
  - [x] Security: workflows use least-privilege `permissions:` (`contents: read`); third-party actions pinned to commit SHAs; `persist-credentials: false`; Gradle wrapper validated by setup-gradle; actionlint clean
  - [x] Flow: end-to-end job runs backend + **fresh** Postgres in Docker (all migrations on an empty database) and starts the admin against it (Playwright browser test comes with WEB-004)
- **Free-tier note (D-010):** private repos get 2,000 Actions minutes/month free. A run that touches everything costs roughly 25 minutes; path filters keep most runs much shorter.
- **Needs from team:** check the first run; turn on branch protection for `main` (Settings → Branches → require PR + status check `ci-ok`).

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
- ⬜ **BE-1xx** Postgres **Row-Level Security** as a second tenant guard: even a buggy query can't return another salon's rows.
  _Needs from team: nothing._
- ⬜ **BE-1xx** Auth: backend verifies **Supabase Auth** tokens (JWKS, ES256), users + roles in our own tables (D-016).
  _Needs from team: a free Supabase project; its project URL + keys sent privately; a Twilio trial account when real SMS is needed._
- ⬜ **BE-1xx** Roles and permissions for the user types in D-012 (owner, stylist, receptionist/manager, customer, internal admin)
- ⬜ **BE-1xx** **Audit log**: an `audit_log` table recording who changed what, when, in which salon, for every create/update/delete.
  Helps settle disputes ("who cancelled this booking?"). _Needs from team: nothing._
- ⬜ **BE-1xx** Seed data: test users for every user type in two test salons (D-012, [TESTING.md](../docs/TESTING.md))

## Pre-launch: Outline only (D-011: nothing is hosted before this phase)

- ⬜ **BE-9xx** Free-tier hosting for backend + Postgres = **staging** (D-010).
  _Needs from team: create accounts on the providers we pick together; add their secrets to GitHub (exact steps given then)._
- ⬜ **BE-9xx** **Uptime monitor** on `/health` with alerts.
  _Needs from team: a free UptimeRobot / Better Stack account and the email/phone that should get alerts._
- ⬜ **BE-9xx** **Automated daily database backups** + one **practice restore** (a backup is only real once restored).
  _Needs from team: a free storage bucket (e.g. Cloudflare R2, already in the stack for invoice PDFs); one teammate to watch the restore drill._
- ⬜ **BE-9xx** Real client IP behind the host's proxy (forwarded headers) so the rate limit works per user. _Needs from team: nothing._
- ⬜ **BE-9xx** Production environment + live keys (Razorpay live, WhatsApp business number). _Needs from team: business KYC on Razorpay and Meta._

## Features: added by the team

<!-- Add feature tasks here using the template in README.md -->
