# ⚙️ Backend + Platform: To Do

Backend = API + database for the app (customer side + salon side, D-023) and the admin panel. API paths per D-017: `/v1/c/...` customer, `/v1/salon/...` salon,
`/v1/admin/...` admin. What to build: [docs/PRODUCT.md](../docs/PRODUCT.md) (D-038).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md)

## Now: Phase 1, Login and accounts

### BE-026 · Fix: emoji names answer 500; tests accept any database error
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** BE-018
- **Why:** project review (2026-10-07): Kotlin counts 😀 as 2 characters and Postgres as 1, so a name at our limit passes our
  check, the database refuses it and the server answers 500; that error's log can include the phone number. Separately,
  `UserRepositoryTest` accepts any `SQLException`, so "relation does not exist" (BE-024) passes it.
- **Needs from team:** nothing.
- **Scope:**
  - text limits counted the way Postgres counts (code points) for every validated text field
  - a database refusal never reaches the client as 500 for input we validate, and database errors never log request data
  - `UserRepositoryTest`'s two database-rule tests check the exact Postgres error (SQL state), like `AdminTablesTest`
- **Done when:**
  - [ ] Tests: a name of emoji exactly at the limit is accepted, one more is refused with `VALIDATION_ERROR`; the two
    database-rule tests fail if the table is missing
  - [ ] Security: no phone numbers or request bodies in database error logs
  - [ ] Database: none
  - [ ] Flow: on the phone, a customer name made of emoji saves

### BE-027 · Fix: Supabase unreachable answers 503, not "sign in again"
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** BE-016
- **Why:** project review (2026-10-07): if Supabase's signing keys can't be fetched, every request gets 401, and the app
  signs everyone out.
- **Needs from team:** nothing.
- **Scope:** keys that can't be fetched (and aren't cached) → 503 `SERVICE_UNAVAILABLE` in the error envelope; a token that
  is really bad stays 401. The app side is APP-014.
- **Done when:**
  - [ ] Tests: keys unreachable → 503; cached keys keep working while Supabase is down; a forged or expired token → 401
  - [ ] Security: a token is never accepted without checking its signature
  - [ ] Database: none
  - [ ] Flow: backend with Supabase blocked → `/v1/me` answers 503; unblocked → 200 without logging in again

### BE-028 · API enums tolerate values an older app doesn't know
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** BE-016
- **Why:** project review (2026-10-07): the shared JSON setup has no fallback for unknown enum values, so adding one
  appointment or payment status would break every older app version.
- **Needs from team:** nothing.
- **Scope:** every enum the app receives decodes an unknown value to a fallback (`UNKNOWN`) instead of failing; the backend
  still refuses unknown values it receives; a contract test lists every enum
- **Done when:**
  - [ ] Tests: a response with a status from the future decodes; the backend still answers 400 to an unknown value sent to it
  - [ ] Security: none beyond input validation staying strict on the backend
  - [ ] Database: none
  - [ ] Flow: the app still logs in and shows the profile

### BE-029 · Remove an admin
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** BE-020, BE-025 · Decisions: D-043, DF-28, DF-29
- **Why:** project review (2026-10-07): an admin can be invited but never removed (`admins.status` is only INVITED / ACTIVE),
  so someone who leaves the team, or whose login is stolen, keeps full access. Needed before WEB-006 invites a second admin.
- **Needs from team:** nothing.
- **Scope:**
  - `GET /v1/admin/admins`: every admin with email, status, who invited them and when (the list WEB-006 shows)
  - `POST /v1/admin/admins/{userId}/remove`: the admin becomes REMOVED; refused for yourself and for the last active admin
    (D-043); removing an already removed admin changes nothing; audit-logged in the same transaction (DF-28)
  - a removed admin's next `/v1/admin` request is refused (the backend already checks the table on every request, DF-29)
  - inviting a removed admin's email again makes them INVITED again (emails are unique)
  - Not included: deleting or banning their Supabase login (it may also be used in the app)
- **Done when:**
  - [ ] Tests: remove → their next admin request is 403; yourself → refused; unknown id → 404; already removed → no change;
    two admins removing each other at the same time leave one active; re-invite → INVITED, then logs in; not an admin or
    no authenticator step → refused (every new route in the "every admin route is guarded" test)
  - [ ] Security: only admins with the authenticator step; the removal and the re-invite are audit-logged
  - [ ] Database: new migration: status REMOVED, `removed_at`, `removed_by`; the status/date checks updated so a removed
    admin who was active stays valid
  - [ ] Flow: on the Supabase database, admin A removes B through the API → B's `/v1/admin/me` is refused

### BE-030 · Fix: audit rows get a request ID the server made; a test that depends on timing
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** BE-020, BE-024
- **Why:** project review (2026-10-07): the backend reuses a client's `X-Request-Id` (so app and server logs line up) and
  writes it into `audit_log.request_id`, so an admin can put any ID they like into the append-only audit log. Separately,
  `ConnectionSchemaTest` waits a fixed 600 ms for HikariCP's 500 ms "check the connection again" window, which can fail at
  random on a slow CI machine.
- **Needs from team:** nothing.
- **Scope:**
  - every request also gets an ID the server makes; audit rows store that one; the client's ID still shows in the logs
  - `ConnectionSchemaTest` no longer depends on a fixed wait (e.g. its test pool checks every connection it hands out)
- **Done when:**
  - [ ] Tests: an admin invite sent with a chosen `X-Request-Id` writes an audit row with a server-made ID; the schema
    test has no sleep
  - [ ] Security: audit rows only hold IDs the server made
  - [ ] Database: none
  - [ ] Flow: an admin invite on the Supabase database writes its audit row; the access log still shows the request ID

### BE-033 · Staff: add and remove, join at first login
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017, BE-022 · Spec: PRODUCT §6.2 · Decisions: D-035, D-039, DF-23
- **Scope:** `GET|POST /v1/salon/staff`, `DELETE /v1/salon/staff/{id}` (owner only, **live salons only**); a number added as
  staff is joined to the salon at its first login and its side becomes SALON (DF-23); a number that already chose CUSTOMER,
  or is in another salon, is refused with a clear error code; removing keeps history (status REMOVED)
- **Done when:**
  - [ ] Tests: adding staff refused unless LIVE; staff joined at first login; a customer's number refused; a number in another
    salon refused; STAFF → 403 on owner routes; salon A can't list or remove salon B's staff
  - [ ] Database: uses BE-017's `salon_members`; no new table
  - [ ] Security: owner-only routes; staff see nothing but their own appointments (D-039)
  - [ ] Flow: on the Supabase database, live "Test Salon A" adds test number B; B's first login lands in the salon

### BE-019 · Audit log
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017 · Spec: PRODUCT §7 (audit log: every admin and money action)
- **Scope:** `audit_log` (who, salon, action, entity, before/after, when); written by every create/update/delete from here on.
  _The table and `AuditLog.record()` already exist since BE-020 (DF-28); this task adds the salon actions._
- **Done when:** tests prove salon create, bank-details change, staff add/remove write audit rows; rows can't be updated or deleted by the app.

### BE-021 · Seed test users
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-017, BE-018, BE-020 · Decision: D-012
- **Needs from team:** the service-role key from BE-020 (creating users in Supabase needs it); the test phone numbers set up in Supabase.
- **Scope:** one command creates Test Salon A + B (both already verified and live) with an owner and two staff each, two
  customers and one admin, all on Supabase test numbers/emails.
- **Done when:** running it twice changes nothing; [docs/TESTING.md](../docs/TESTING.md) lists the logins (codes live in the password manager).

### BE-022 · Salon verification by our admins
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-032, BE-019, BE-020 · Decisions: D-033, DF-24
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
- ⬜ **BE-9xx** Database timeouts: a statement timeout and a socket timeout on the backend's connections, so a slow database
  or a full connection pool answers 503 quickly instead of a 500 after ~15 s (review 2026-10-07).
- ⬜ **BE-9xx** Production + live keys (Razorpay live, WhatsApp business number). _Needs from team: Razorpay + Meta business KYC; a CA's advice on GST/TCS (PRODUCT §8.2); rotate the dev Supabase secret key (it was shared in a chat; team will do it)._
