# ⚙️ Backend + Platform: To Do

Backend = API + database for the app (customer side + salon side, D-023) and the admin panel. API paths per D-017: `/v1/c/...` customer, `/v1/salon/...` salon,
`/v1/admin/...` admin. What to build: [docs/PRODUCT.md](../docs/PRODUCT.md) (D-038).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md)

## Now: Phase 1, Login and accounts

### BE-017 · Salons, bank details, staff, roles and tenant isolation
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Spec: PRODUCT §3, §6.1, §6.2 · Decisions: D-025–D-027, D-033, D-035, D-036, D-039, DF-23, DF-24
- **Needs from team:** an encryption key for bank
  details put into `.env` by the team (the task report says how to make one).
- **Scope:**
  - `salons` (name, phone, address, type men/women/unisex, **status** DRAFT → UNDER_VERIFICATION → LIVE, or REJECTED with a reason, or SUSPENDED)
  - `salon_bank_details` (account holder name, account number **encrypted**, IFSC); masked in every app response (DF-24)
  - `salon_members` (salon, phone, user (filled in at that number's first login), role OWNER / STAFF (D-039), status ACTIVE / REMOVED)
  - `POST /v1/salon/salons` (creator becomes OWNER) · `PUT /v1/salon/bank-details` · `POST /v1/salon/submit-for-verification` ·
    `GET /v1/salon/me/salons` · `GET|POST /v1/salon/staff`, `DELETE /v1/salon/staff/{id}` (owner only, **live salons only**)
  - a number added as staff is joined to the salon at its first login and its side becomes SALON (DF-23); a number that already
    chose CUSTOMER is refused with a clear error code
  - one salon per person (D-035): a phone that is already in another salon can't be added; every salon route takes the salon from
    the signed-in person's membership and checks the role (D-036); everything except a staff member's own appointments is OWNER-only (D-039);
    STAFF reach only their own appointments (D-039)
  - row-level security (D-027): Flyway keeps the owner user; the backend's queries run as a new limited database user (not superuser,
    not owner; created by the local setup and Testcontainers, password from env); each transaction sets `app.salon_id`
- **Done when:**
  - [ ] Tests: create salon → submit → UNDER_VERIFICATION; adding staff refused unless LIVE; staff joined at first login; a
    customer's number refused; STAFF → 403 on owner routes and on other staff's appointments; **salon A can't read or change salon B** (every route);
    adding a phone already in another salon → refused
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
- **Scope:** one command creates Test Salon A + B (both already verified and live) with an owner and two staff each, two
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
