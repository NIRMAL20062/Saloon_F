# ⚙️ Backend + Platform: To Do

Backend = API + database for both apps and the admin panel. API paths per D-017: `/v1/c/...` customer, `/v1/salon/...` salon,
`/v1/admin/...` admin. Flows from [ChatGPT.md](../ChatGPT.md) and [Salon_App_Task_Wise_Development.md](../Salon_App_Task_Wise_Development.md).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md)

## Now: Phase 1, Login and accounts

### BE-016 · Backend trusts Supabase logins + `GET /v1/me`
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-011 · Decision: D-016
- **Why:** the apps log in with Supabase; the backend must check every request's token itself.
- **Needs from team:** Supabase project (free, region Mumbai) with Phone provider + Twilio + test phone numbers; project URL shared;
  secret keys put into `.env` by the team (never in chat or git). Steps given in the task report.
- **Scope:**
  - verify the Supabase access token on every protected route: signature (JWKS public keys), issuer, audience, expiry
  - `app_users` table (one row per Supabase user: id, phone, created_at), created on first request
  - `GET /v1/me` → who is signed in
- **Done when:**
  - [ ] Tests: valid token → 200; missing / expired / wrong-signature / wrong-issuer token → 401 with the error envelope
  - [ ] Database: migration for `app_users`; first-request creation is idempotent (two parallel first requests → one row)
  - [ ] Security: tokens never logged; JWKS cached with a timeout; no Supabase secret in the repo
  - [ ] OpenAPI spec updated; contract test passes

### BE-017 · Salons, members, roles and tenant isolation
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Spec: E1.3, E1.4, E2.1, E3.2
- **Needs from team:** nothing; permissions follow the table in the salon plan §1 (DF-17, veto any cell).
- **Scope:**
  - `salons` (name, phone, address, type men/women/unisex), `salon_members` (user, salon, role OWNER / MANAGER / STYLIST, status), `salon_invites` (phone, role, expiry)
  - `POST /v1/salon/salons` (creator becomes OWNER) · `GET /v1/salon/me/salons` · `POST /v1/salon/staff/invites` (owner only) · `POST /v1/salon/invites/{id}/accept`
  - every salon route takes the salon from the member's selection and checks membership + role; Postgres row-level security as a second guard
- **Done when:**
  - [ ] Tests: create salon; invite → accept only by the invited phone; **salon A can't read or change salon B** (every route); wrong role → 403
  - [ ] Database: migrations with constraints (one OWNER minimum, unique member per salon, invite expiry), RLS policies tested
  - [ ] Security: salon id never trusted from the request body; audit-logged once BE-019 lands

### BE-018 · Customer profile API
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Spec: C1.2, data model `customers_app_users`
- **Scope:** `GET /v1/c/me`, `PUT /v1/c/me` (name required 2–60 chars, email optional + valid); `customers_app_users` table linked to `app_users`.
- **Done when:** tests for validation, own-profile only, new vs returning customer; OpenAPI updated.

### BE-019 · Audit log
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017 · Spec: E1.5; spec v2 "audit entry for every admin or system money action"
- **Scope:** `audit_log` (who, salon, action, entity, before/after, when); written by every create/update/delete from here on.
- **Done when:** tests prove salon create, invite, accept write audit rows; rows can't be updated or deleted by the app.

### BE-020 · Admins: first admin, invites, admin-only routes
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Decisions: D-013, DF-16
- **Needs from team:** the email of the first admin; Email provider turned on in Supabase (+ free SMTP, e.g. Brevo) and authenticator-app MFA enabled.
- **Scope:** `admins` table (our DB decides who is admin); first admin created by a one-off command; `GET /v1/admin/me`;
  `POST /v1/admin/admins/invites` (sends the Supabase invite email); every `/v1/admin` route requires an admin **with MFA completed**.
- **Done when:** tests: non-admin → 403; admin without MFA → 403; invite creates a pending admin; audit-logged.

### BE-021 · Seed test users
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017, BE-018, BE-020 · Spec: E1.6, D-012
- **Scope:** one command creates Test Salon A + B with owner, manager, stylist each, two customers and one admin, all on Supabase test numbers/emails.
- **Done when:** running it twice changes nothing; [docs/TESTING.md](../docs/TESTING.md) lists the logins (codes live in the password manager).

## Next phases (outline)

- **Phase 2** BE-1xx working hours, closed days · services (duration, price in paise) · staff hours, leave · salon customers list (salon plan E2–E4)
- **Phase 3** BE-2xx availability engine · appointments with **no double booking** (database-enforced) · statuses + state machine · push to salon staff (E5, E6.1)
- **Phase 4** BE-3xx listing + geo search + filters · slot lock (10 min) · customer bookings, reschedule/cancel with policy engine · WhatsApp + reminders (EC2–EC3, E6–E7)
- **Phase 5** BE-4xx bills, payments at counter, payment links, PDF invoices (E8–E9)
- **Phase 6** BE-5xx Razorpay Route orders with hold, verified webhooks, refunds, double-entry ledger, hourly reconciliation, subscriptions (EC4, E12)
- **Phase 7** BE-6xx disputes state machine + SLAs, payout freeze, chargebacks, reviews, reliability scores, reports (EC5–EC6, E10)

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
- ⬜ **BE-9xx** Production + live keys (Razorpay live, WhatsApp business number). _Needs from team: Razorpay + Meta business KYC; a CA's advice on GST/TCS (spec v2)._
