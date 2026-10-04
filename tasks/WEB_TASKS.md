# 🖥️ Admin Web Panel: To Do

Internal admin panel **for our team only** (D-002). Next.js + TypeScript in `admin/`. Admin features come from
[docs/PRODUCT.md](../docs/PRODUCT.md) §7 (D-038).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [WEB_TASKS_COMPLETED.md](WEB_TASKS_COMPLETED.md)

> ⚠️ Not deployable on a public URL until admin login (WEB-005) exists.

## Now: Phase 1, Admin login and salon verification

### WEB-005 · Admin login: email code + authenticator app
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** WEB-002, BE-020 · Decisions: D-013, D-016, DF-16
- **Needs from team:** same Supabase project; the first admin's email.
- **Flow:** email → 6-digit code from the email → first time: set up an authenticator app (QR) → enter its code → admin home.
  Everything under the panel requires login; session expires after inactivity; logout.
- **Done when:**
  - [ ] Tests: every screen state; logged-out user is redirected to login from every page; non-admin account sees "no access"
  - [ ] Security: Supabase is called only from the Next.js server (DF-20); session cookie httpOnly + secure + sameSite; no tokens in the browser's JavaScript; MFA required
  - [ ] Flow: in the browser, the first admin logs in end to end; a non-admin email is refused

### WEB-006 · Admins page: invite more admins
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** WEB-005 · Decision: D-013
- **Flow:** Admins → list (name, email, added by, date) → "Invite admin" → email → they receive the invite and log in (WEB-005 flow).
- **Needs from team:** Supabase's invite email links to a browser login, which our admin website doesn't use (DF-20). Change
  its text (Authentication → Emails → Invite user) to "Open the Glide admin website and log in with this email"; real emails
  also need the email service (e.g. Brevo).
- **Done when:** tests for list/invite/errors; invite audit-logged; in the browser, admin A invites B and B logs in.

### WEB-007 · Verify salons
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** WEB-005, BE-022 · Decisions: D-033, DF-24
- **Why:** a salon goes live only after our team has checked its profile and bank details.
- **Flow:** Salons waiting → open one → profile + bank details → **Approve**, or **Reject** with a reason → the list updates; the
  owner's app shows the result.
- **Done when:** tests for list / detail / approve / reject (reason required) / errors; bank details shown only on the detail page
  and never stored in the browser; in the browser, an admin approves Test Salon A and the owner's app shows it live.

## Next phases (outline)

- **Phase 2+** WEB-1xx Salons list + detail (members, plan, activity), suspend/reactivate · **design-partner tracking** (logins, appointments, bills per week; never cut) · audit log viewer · support lookup by phone
- **Phase 6** WEB-5xx payments explorer + manual refund with reason · payout monitoring · GMV / commission / refunds dashboard · subscription price editing (D-032, DF-25)
- **Phase 7** WEB-6xx **dispute console**: queue, case view, decision, appeal, templates, money panel (PRODUCT §7, §9) · refund-policy settings · customer management · review moderation (salon approval is WEB-007)

## Platform backlog (done alongside features; never blocks them)

- ⬜ **WEB-003** System status page (Phase 0 proof; optional now that real admin pages are coming)
- ⬜ **WEB-004** Playwright browser tests in CI: first for the WEB-005 login flow against the real backend

## Pre-launch (outline)

- ⬜ **WEB-9xx** Admin panel on free hosting, reachable only after login. _Needs from team: a free hosting account (e.g. Vercel)._
