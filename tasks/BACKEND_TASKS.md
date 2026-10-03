# ⚙️ Backend + Platform: To Do

Backend = API + database for the app (customer side + salon side, D-023) and the admin panel. API paths per D-017: `/v1/c/...` customer, `/v1/salon/...` salon,
`/v1/admin/...` admin. What to build: [docs/PRODUCT.md](../docs/PRODUCT.md) (D-038).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md)

## Now: Phase 1, Login and accounts

### BE-016 · Backend trusts Supabase logins + `GET /v1/me`
- **Phase:** 1 · **Status:** 🔄 Built and verified, waiting for the team's OK · **Owner:** Claude · **Depends on:** BE-011 · Decision: D-016 · **Commits:** `3c507d6` `1468aed`
- **Why:** the apps log in with Supabase; the backend must check every request's token itself.
- **Needs from team:** Supabase project (free, region Mumbai) with Phone provider + Twilio + test phone numbers; project URL shared;
  secret keys put into `.env` by the team (never in chat or git). Steps given in the task report.
- **Scope:**
  - verify the Supabase access token on every protected route: signature (JWKS public keys), issuer, audience, expiry
  - `app_users` table (one row per Supabase user: id, phone, **side** CUSTOMER / SALON / not chosen yet, created_at), created on first request
  - `GET /v1/me` → who is signed in and their chosen side (the app picks the interface from this, D-024). The person's salon and role are added by BE-017, when salons exist (new fields; older app versions ignore them)
  - `PUT /v1/me/side` → save the onboarding choice (customer or salon). **Final once chosen** (D-030, team 2026-10-03): a different side → 409 `SIDE_ALREADY_CHOSEN`, the same side again → 200; a database trigger refuses any change too
- **Done when:**
  - [x] Tests: valid token → 200; missing / expired / wrong-signature / wrong-issuer token → 401 with the error envelope; side saved and returned (`SupabaseTokenVerifierTest` 9, `MeRoutesTest` 6)
  - [x] Database: `V2__app_users.sql`; first-request creation is idempotent: 10 parallel first requests → one row (`UserRepositoryTest`, real Postgres); side enforced by a CHECK constraint; `V3__app_users_side_final.sql`: a trigger refuses changing a chosen side (D-030). A new migration, not an edit of V2, so local databases that already ran V2 keep working
  - [x] Security: tokens and phone numbers never logged (checked in the real run's log); JWKS cached, 5 s download timeout, 5 s retry pause; only ES256/RS256 accepted (HS256 forgery refused); no Supabase secret in the repo or used by the backend
  - [x] OpenAPI spec updated (`/v1/me`, `/v1/me/side`, bearer security, 401 envelope); contract tests pass
  - [x] Flow (real Supabase project): test number OTP → real access token → `GET /v1/me` 200 → `PUT /v1/me/side` CUSTOMER saved and returned → tampered token 401. After D-030 (2026-10-03, on the local database that had run V2): V3 applied without a reset; same side again → 200, other side → 409 `SIDE_ALREADY_CHOSEN`, side unchanged

### BE-017 · Salons, bank details, staff, roles and tenant isolation
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Spec: PRODUCT §3, §6.1, §6.2 · Decisions: D-025–D-027, D-033, D-034, DF-23, DF-24
- **Needs from team:** an encryption key for bank
  details put into `.env` by the team (the task report says how to make one).
- **Scope:**
  - `salons` (name, phone, address, type men/women/unisex, **status** DRAFT → UNDER_VERIFICATION → LIVE, or REJECTED with a reason, or SUSPENDED)
  - `salon_bank_details` (account holder name, account number **encrypted**, IFSC); masked in every app response (DF-24)
  - `salon_members` (salon, phone, user (filled in at that number's first login), role OWNER / MANAGER / STAFF, status ACTIVE / REMOVED)
  - `POST /v1/salon/salons` (creator becomes OWNER) · `PUT /v1/salon/bank-details` · `POST /v1/salon/submit-for-verification` ·
    `GET /v1/salon/me/salons` · `GET|POST /v1/salon/staff`, `DELETE /v1/salon/staff/{id}` (owner/manager, **live salons only**)
  - a number added as staff is joined to the salon at its first login and its side becomes SALON (DF-23); a number that already
    chose CUSTOMER is refused with a clear error code
  - one salon per person (D-035): a phone that is already in another salon can't be added; every salon route takes the salon from
    the signed-in person's membership and checks the role (D-036); only the OWNER changes bank details and the subscription (D-037);
    STAFF reach only their own bookings (D-034)
  - row-level security (D-027): Flyway keeps the owner user; the backend's queries run as a new limited database user (not superuser,
    not owner; created by the local setup and Testcontainers, password from env); each transaction sets `app.salon_id`
- **Done when:**
  - [ ] Tests: create salon → submit → UNDER_VERIFICATION; adding staff refused unless LIVE; staff joined at first login; a
    customer's number refused; STAFF → 403 on owner/manager routes; **salon A can't read or change salon B** (every route);
    a MANAGER changing bank details → 403; adding a phone already in another salon → refused
  - [ ] Database: migrations with constraints (exactly one OWNER, unique phone per salon, valid statuses, IFSC format); RLS
    enabled + forced; test: the limited user with salon A set sees zero rows of salon B, and with no salon set sees zero rows
  - [ ] Security: account number encrypted at rest, masked in responses, never logged; salon id never trusted from the request
    body; audit-logged once BE-019 lands

### BE-018 · Profile: name and email
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Spec: PRODUCT §5 · Decision: DF-18
- **Scope:** `name` and `email` columns on `app_users` (new migration; one profile per person, no `customers_app_users` table);
  `GET /v1/me` returns them; `PUT /v1/me/profile` (name required 2–60 chars, email optional + valid).
  `PUT /v1/me/side` refuses a change once a side is saved: the choice is final (D-030). The team asked for this fix in BE-016
  (PR #2); if it's already there, only check it here.
- **Done when:** tests for validation, own profile only, new vs returning user, a different side after one is saved → 409; OpenAPI updated.

### BE-019 · Audit log
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017 · Spec: PRODUCT §7 (audit log: every admin and money action)
- **Scope:** `audit_log` (who, salon, action, entity, before/after, when); written by every create/update/delete from here on.
- **Done when:** tests prove salon create, bank-details change, staff add/remove write audit rows; rows can't be updated or deleted by the app.

### BE-020 · Admins: first admin, invites, admin-only routes
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Decisions: D-013, DF-16
- **Needs from team:** the email of the first admin; Email provider turned on in Supabase (+ free SMTP, e.g. Brevo) and authenticator-app MFA enabled;
  the Supabase **service-role key** put into `.env` by the team (the backend needs it to send invites; never in chat or git).
- **Scope:** `admins` table (our DB decides who is admin); first admin created by a one-off command; `GET /v1/admin/me`;
  `POST /v1/admin/admins/invites` (sends the Supabase invite email); every `/v1/admin` route requires an admin **with MFA completed**.
- **Done when:** tests: non-admin → 403; admin without MFA → 403; invite creates a pending admin; audit-logged.

### BE-021 · Seed test users
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017, BE-018, BE-020 · Decision: D-012
- **Needs from team:** the service-role key from BE-020 (creating users in Supabase needs it); the test phone numbers set up in Supabase.
- **Scope:** one command creates Test Salon A + B (both already verified and live) with owner, manager and staff each, two
  customers and one admin, all on Supabase test numbers/emails.
- **Done when:** running it twice changes nothing; [docs/TESTING.md](../docs/TESTING.md) lists the logins (codes live in the password manager).

### BE-022 · Salon verification by our admins
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017, BE-019, BE-020 · Decisions: D-033, DF-24
- **Scope:** `salon_verifications` (salon, admin, decision APPROVED / REJECTED, reason, when) ·
  `GET /v1/admin/salons?status=UNDER_VERIFICATION` · `GET /v1/admin/salons/{id}` (profile + full bank details; every view
  audit-logged) · `POST /v1/admin/salons/{id}/approve` · `POST /v1/admin/salons/{id}/reject` (reason required) → the salon becomes
  LIVE or REJECTED, and the owner sees it in `GET /v1/salon/me/salons`.
- **Done when:** tests: non-admin → 403; approve/reject only from UNDER_VERIFICATION; reject without a reason → 400; every
  decision and every bank-details view writes an audit row; OpenAPI updated.

## Next phases (outline)

- **Phase 2** BE-1xx working hours, closed days · services (duration, price in paise) · staff hours, leave · salon customers list (PRODUCT §6.3)
- **Phase 3** BE-2xx availability engine · appointments with **no double booking** (database-enforced) · statuses + state machine · staff accept their own bookings · push to salon staff (PRODUCT §6.4)
- **Phase 4** BE-3xx listing + geo search + filters · slot lock (10 min) · customer bookings, reschedule/cancel with policy engine · WhatsApp + reminders (PRODUCT §5, §10)
- **Phase 5** BE-4xx bills, PDF invoices (PRODUCT §6.5); no cash: bills are paid online from Phase 6 (D-029)
- **Phase 6** BE-5xx all online money through our Razorpay (D-028, D-029): salon linked accounts (KYC), Route orders with hold, bill payments by link / UPI QR at the counter + day summary, platform fee (Q-013), verified webhooks, refunds, double-entry ledger, hourly reconciliation, subscriptions: one plan, ₹179–₹400 a month, price editable by admins (D-032, DF-25) (PRODUCT §8)
- **Phase 7** BE-6xx disputes state machine + SLAs, payout freeze, chargebacks, reviews, reliability scores, reports (PRODUCT §6.6, §9)

## Platform backlog (done alongside features; never blocks them)

- ⬜ **BE-012** Security scanning: CodeQL (Kotlin + TypeScript), Dependabot (Gradle, npm, Actions, Docker), dependency review on PRs,
  secret scanning. _Needs from team: Settings → Code security → turn on Secret scanning + Push protection (free on public repos)._
- ⬜ **BE-013** CD: on merge to `main` publish the backend image to GitHub Container Registry and the Android builds to Firebase App
  Distribution (skips until Firebase secrets exist); deploy step at Pre-launch.

## Pre-launch (outline; D-011: nothing is hosted before this phase)

- ⬜ **BE-9xx** Free-tier hosting for backend + Postgres = staging. _Needs from team: provider accounts, secrets added to GitHub._
- ⬜ **BE-9xx** Uptime monitor on `/health`. _Needs from team: free UptimeRobot / Better Stack account._
- ⬜ **BE-9xx** Daily database backups + one practice restore. _Needs from team: a free storage bucket (Cloudflare R2)._
- ⬜ **BE-9xx** Real client IP behind the host's proxy so rate limiting works per user.
- ⬜ **BE-9xx** Production + live keys (Razorpay live, WhatsApp business number). _Needs from team: Razorpay + Meta business KYC; a CA's advice on GST/TCS (PRODUCT §8.2); rotate the dev Supabase secret key (it was shared in a chat; team will do it)._
