# 🖥️ Web Admin Panel: Completed

Newest at the bottom. Each entry keeps the commits so anyone can `git show <hash>` to see exactly what changed.

> Groundwork the panel already relies on (done in the backend/platform files):
> [BE-007](BACKEND_TASKS_COMPLETED.md#be-007--health-readiness-endpoint) `/health` endpoint ·
> [BE-008](BACKEND_TASKS_COMPLETED.md#be-008--security-baseline--standard-error-envelope) error envelope and CORS

## Phase 0: Walking skeleton

### WEB-001 · Next.js admin skeleton
- **Completed:** 2026-10-02 · **Commits:** `f347192` `e132055` `be02f86` `ac9be71`
- **Built:** `admin/` from create-next-app 16.3.8 (TypeScript, App Router, Tailwind 4, ESLint, pnpm); `src/env.ts` (zod, server-only `API_BASE_URL`), `src/instrumentation.ts` (invalid config → clear message + exit 1), `src/proxy.ts` + `src/security/` (per-request nonce CSP, static security headers, no `X-Powered-By`), every page dynamic so the nonce applies; `pnpm verify` = lint + typegen/tsc + Vitest + build; `admin/CLAUDE.md`
- **Tests:** `page.test.tsx` (1), `env.test.ts` (3), `security.test.ts` (5) = 9
- **Verified (Claude):** `pnpm dev` and `pnpm start` serve "Glide Admin" (200); on `next start` every page script carries that request's nonce and the nonce changes per request; bad `API_BASE_URL` → exit 1 with the problem listed. Team said continue on 2026-10-02
- **Security:** CSP without `unsafe-inline` (eval only in dev), `frame-ancestors 'none'`, X-Frame-Options DENY, nosniff, no-referrer, Permissions-Policy, noindex, HSTS in production; backend URL never reaches the browser
- **Database:** none

## Phase 1: Admin login and salon verification

### WEB-002 · Typed API client
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** BE-010
- **Why:** admin pages call the backend with types generated from `docs/api/openapi.yaml`, so nothing is hand-copied.
- **Done when:**
  - [x] TypeScript types generated from the spec; small typed fetch client used only on the server
  - [x] backend error envelope mapped to one error type; tests for success, error envelope, backend down
  - [x] changing the spec without regenerating fails `pnpm verify`
- **Completed:** 2026-10-04 · **Commits:** `e7c0b22` `dfa6aee` `91a81fb` `4e0f466` · PR #10
- **Built:** `openapi-typescript` 7.13.0 generates `admin/src/api/schema.d.ts` from `docs/api/openapi.yaml` (`pnpm gen:api`);
  `pnpm verify` starts with `check:api` (fails when the committed types differ from the spec). `src/api/client.ts` (server only):
  `openapi-fetch` 0.17.0 with the generated paths, 10 s timeout, `redirect: "error"`; `unwrap()` returns the typed body or
  throws `ApiError` (`src/api/errors.ts`): backend `error.code` + status + request ID, `BACKEND_UNREACHABLE` (no answer /
  timeout), `UNEXPECTED_RESPONSE` (no envelope, empty body, broken JSON). The backend's message is not kept. DF-27
- **Tests:** `client.test.ts` (10, incl. a compile-time check that calls outside the spec don't build); admin total 20
- **Verified (Claude):** against the real backend on Supabase's Postgres, `GET /health` typed `{UP, database UP}`; `GET /v1/me`
  without / with a bad token → `ApiError UNAUTHORIZED 401` with request ID; editing an enum in the spec made `check:api` fail.
  No phone test (admin-only, nothing visible). Team OK on 2026-10-04
- **Security:** `server-only`, no `NEXT_PUBLIC_` vars, no logging, backend error text never reaches pages, no redirects followed
  (tokens can't leak to another host); `pnpm audit --prod` clean
- **Database:** none

### WEB-005 · Admin login: email code + authenticator app
- **Completed:** 2026-10-07 (team's OK) · **Commits:** `d0b2f2e` `b353724` `a1cd835` `27b95bd` `f665e58` `59014f1` `1ad9e7e` `b1b5f7d` `2465e9f` `4a0ad4e` `313091c` + this one · PR #12
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** WEB-002, BE-020 · Decisions: D-013, D-016, DF-16
- **Needs from team:** same Supabase project; the first admin's email. Done 2026-10-07: custom SMTP via Brevo (Supabase's
  built-in email allows ~2 a hour; Brevo's IP blocking turned off for SMTP keys, since Supabase's servers change address).
  Magic Link email template (Authentication → Emails), code only, because a link logs in through the browser, which the
  admin website doesn't use (DF-20), and redirects to the Site URL:
  subject `Your Glide Admin login code`; body: "Enter this code on the Glide Admin login page: `{{ .Token }}`. It works for
  10 minutes and only once. If you didn't try to log in, ignore this email: Glide Admin also asks for the code from the
  authenticator app."
- **Flow:** email → 6-digit code from the email → first time: set up an authenticator app (QR) → enter its code → admin home.
  Everything under the panel requires login; session expires after inactivity; logout.
- **Done when:**
  - [x] Tests: every screen state; logged-out user is redirected to login from every page; non-admin account sees "no access"
    (Vitest: 127 admin tests, incl. `guard.test.ts`, `actions.test.ts`, `pages.test.tsx`, `forms.test.tsx`)
  - [x] Security: Supabase is called only from the Next.js server (DF-20); session cookie httpOnly + secure + sameSite; no tokens in the browser's JavaScript; MFA required
    (checked in the browser too: `__Host-` cookie httpOnly/Secure/Strict, `document.cookie` empty, no token in pages or JS bundles)
  - [x] Flow: in the browser, the first admin logs in end to end; a non-admin email is refused
    (headless Chrome, real Supabase + backend, 2026-10-07: the team member's email, invited by `admin@glide.test`: email
    code → authenticator setup → home → logout → second login with the authenticator code; an email without a login gets
    the same screens and every code is refused. "No access" for a non-admin *with* a login: unit tests + BE-020's backend
    check; no such email account to try in the browser)
- **Built:** login screens (`/login`, `/login/code`, `/login/mfa/setup`, `/login/mfa`, `/login/continue`, `/no-access`) with
  Server Actions; `src/auth/`: encrypted `__Host-` session cookie (`jose`, AES-256-GCM), server-only Supabase Auth REST
  client, `guard()` used by `proxy.ts` (login required everywhere, 30 min idle / 12 h, token refresh), `requireAdmin()`
  (asks the backend) for every admin page; admin home with logout; `app/error.tsx`; email codes 6–10 digits (dev sends 8).
  New settings `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `ADMIN_SESSION_SECRET`; new dependency `jose` 6.2.12; DF-30
- **Tests:** `actions.test.ts` 28, `guard.test.ts` 20, `supabase-auth.test.ts` 18, `pages.test.tsx` 14, `forms.test.tsx` 10,
  `session.test.ts` 8, `require-admin.test.ts` 5, `proxy.test.ts` 3, `env.test.ts` +2; admin total 127. CI smoke checks
  that `/` redirects to `/login`
- **Security:** Supabase only from the Next.js server; no tokens in browser JavaScript or JS bundles (checked); cookie httpOnly,
  Secure, SameSite=strict, `__Host-`; MFA required (proxy + `requireAdmin()` + backend `aal2`); non-admins signed out before
  any authenticator; emails without a login can't be told apart; our own error words. Known gap: login rate limits before
  hosting (WEB-9xx)
- **Database:** none
- **Verified by:** Claude in headless Chrome against real Supabase (Brevo SMTP) + local backend, 2026-10-07 (see Done when);
  found and fixed 8-digit email codes and the silent bounce after a good code; found BE-024

### WEB-008 · Fix: admin login security follow-up (from the WEB-005 re-check)
- **Completed:** 2026-10-07 (team's go-ahead, merged) · **Commits:** `1c83018` `5e44677` `0db5018` `9001708` `8982355` `bbd23ac` `5f64b2e`
- **Phase:** 1 · **Status:** ✅ Done · **Owner:** Claude · **Depends on:** WEB-005 · Decisions: DF-20, DF-30
- **Why:** a re-check of WEB-005 (2026-10-07, finished just after it merged) proved two holes on a running copy of the admin
  website: an idle session could still open admin pages by adding a prefetch header, and a token the backend refuses sent
  the browser round `/` → `/login?expired=1` → `/` without end.
- **Needs from team:** nothing.
- **Scope:**
  - `admin/src/proxy.ts` no longer skips requests with `purpose: prefetch` / `next-router-prefetch`: prefetches are checked
    like any page request. Browser prefetches (`Sec-Purpose`) don't count as activity; Next.js's own link prefetches can't
    be told apart (Next removes their headers before the proxy) and count like a visit, as they follow a hover or scroll.
  - The matcher's `/api` exclusion no longer skips pages that merely start with "api" (e.g. `/api-keys`).
  - `readSession()` / `requireAdmin()` refuse sessions idle for 30 minutes or older than 12 hours themselves (DF-30), so
    pages don't rely on the proxy alone.
  - The proxy deletes the session cookie on `/login?expired=1`, and the login page never sends a visitor back to `/` when
    `expired` is in the address.
  - `admin/CLAUDE.md`: the proxy skips `/api`, so any future `/api` route must call `requireAdmin()` itself.
  - Not included: login rate limiting (known gap in docs/SECURITY.md); any other change to the WEB-005 flow.
- **Done when:**
  - [x] Tests: the proxy's matcher runs on prefetch requests (and still skips `/api` and static files); an idle session's
    prefetch is sent to log in, and a browser prefetch doesn't record activity; `/api-keys` is covered; `readSession()` gives nothing for an idle or
    too-old session and `requireAdmin()` then sends to `/login?expired=1`; `/login?expired=1` deletes the session cookie;
    the login page with `expired` doesn't redirect to `/`
  - [x] Security: no admin page or Server Action works with an idle or too-old session, whatever the headers; a token the
    backend refuses ends on the login page, never in a loop
  - [x] Database: none
  - [x] Flow: in the browser, against the real backend + Supabase: an idle session gets the login page with and without a
    prefetch header; a session whose token the backend refuses ends on the login page with the "logged out" message and
    no loop; logging in again works
- **Tests:** admin total 148 (+21): `proxy.test.ts` (matcher via Next's `unstable_doesMiddlewareMatch`: prefetches covered,
  `/api` and static files skipped, `/api-keys` covered; idle session with each prefetch header; browser prefetch isn't
  activity; `/login?expired=1` deletes the cookie), `guard.test.ts` (prefetch limits and activity; logged-out page),
  `cookies.test.ts` (new: `readSession()` limits, `hasSessionCookie()`), `require-admin.test.ts` (stale cookie),
  `pages.test.tsx` (no bounce with `expired`). Also fixed a race in `client.test.ts`'s hanging-backend fake (flaky timeout)
- **Security:** `/feature-security-check` PASS. Every request is checked, prefetches included; stale sessions are refused at the
  proxy, `readSession()` and `requireAdmin()`. The cookie deletion on `/login?expired=1` can't be triggered from another site
  (the SameSite=strict cookie isn't sent cross-site, and nothing is deleted without it)
- **Database:** none
- **Notes:** a project review (2026-10-07) found that Next.js removes `next-router-prefetch` and `rsc` before the proxy runs
  (confirmed in `next/dist/server/web/adapter.js`), so Next's own link prefetches count as activity (they follow a hover or
  scroll); only browser prefetches (`Sec-Purpose`) don't. Same review: the `/api` exclusion also skipped pages like `/api-keys`
- **Verified by:** Claude in headless Chrome against the local admin website (WEB-008 code, a throwaway session key so test
  cookies could be made) + backend + real Supabase, around a real `admin@glide.test` login (email code from the admin API +
  test authenticator; no email sent): 14 checks passed. An idle session gets 307 to `/login?expired=1` with its cookie deleted
  and no admin data, with each prefetch header and without; a browser prefetch doesn't record activity, a page view and
  Next's router prefetch do; a token the backend refuses → `/login?expired=1` after 2 page loads (no loop), message + email
  form shown, cookie gone; a new login afterwards opens the admin home. The email step itself wasn't sent in the browser
  (`admin@glide.test` has no inbox); it is unchanged from WEB-005
