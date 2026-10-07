# 🖥️ Admin Web Panel: To Do

Internal admin panel **for our team only** (D-002). Next.js + TypeScript in `admin/`. Admin features come from
[docs/PRODUCT.md](../docs/PRODUCT.md) §7 (D-038).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [WEB_TASKS_COMPLETED.md](WEB_TASKS_COMPLETED.md)

> ⚠️ Not deployable on a public URL until admin login (WEB-005) exists.

## Now: Phase 1, Admin login and salon verification

### WEB-009 · Fix: logout and the time limits also end the Supabase login
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** WEB-008
- **Why:** project review (2026-10-07): logout and the 30 min / 12 h limits only delete the cookie. If the session had
  already timed out, Supabase isn't told, so a copied cookie's refresh token stays usable.
- **Needs from team:** nothing.
- **Scope:** logout signs the login out at Supabase even when its access token has expired (refresh first if needed); a
  session ended by the time limits is signed out at Supabase too (best effort). Not included: revoking access tokens before
  they expire (at most 1 hour; the backend checks them itself).
- **Done when:**
  - [ ] Tests: logout with an expired access token still signs out at Supabase; an idle or too-old session is signed out
    when the proxy ends it; a Supabase error never blocks the logout
  - [ ] Security: after logout, the old refresh token is refused by Supabase
  - [ ] Database: none
  - [ ] Flow: in the browser: log in, copy the cookie, log out → the copied cookie can't refresh

### WEB-010 · Fix: the login page doesn't reveal which emails have a login
- **Phase:** 1 · **Status:** ⬜ To do · **Owner:** - · **Depends on:** WEB-005
- **Why:** project review (2026-10-07): asking for a code twice gives "too many attempts" only for emails that have a login
  (Supabase's own per-user limit), so admin emails can be discovered.
- **Needs from team:** nothing.
- **Scope:** our own "wait a minute before the next code" rule for every email, so the screens and messages are the same
  whether or not the email has a login. Login rate limits per IP stay in the Pre-launch hosting task.
- **Done when:**
  - [ ] Tests: a second request within a minute gets the same message for an admin email and an unknown email
  - [ ] Security: no difference in screens or messages between an admin email and an unknown one
  - [ ] Database: none
  - [ ] Flow: in the browser, both emails twice in a row → the same screens

### WEB-006 · Admins page: invite more admins
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** WEB-005 · Decision: D-013
- **Flow:** Admins → list (name, email, added by, date) → "Invite admin" → email → they receive the invite and log in (WEB-005 flow).
- **Needs from team:** real emails need the email service (e.g. Brevo). The invite email's text was changed by the team on
  2026-10-04 (Supabase's default links to a browser login, which our admin website doesn't use, DF-20).
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
- ⬜ **WEB-004** Playwright browser tests in CI: first for the WEB-005 login flow against the real backend. **Moved up** by the
  project review (2026-10-07): do it after WEB-010, together with BE-012 (dependency scanning)

## Pre-launch (outline)

- ⬜ **WEB-9xx** Admin panel on free hosting, reachable only after login, **with its own login rate limits** (per client IP and
  per email; SECURITY.md known gap). _Needs from team: a free hosting account (e.g. Vercel)._
