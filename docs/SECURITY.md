# Security

Per-feature checklist: run `/feature-security-check` (see `.claude/skills/feature-security-check/SKILL.md`) before every PR.

## Product rules (from the plan, non-negotiable)

1. The app **never** calls WhatsApp directly; only the backend does.
2. Razorpay orders are **created** on the backend (amount computed server-side) and payments are **confirmed only by a
   verified webhook signature** (or server-side verification), never by the app's success callback alone.
3. **Multi-tenant from day 1**: every salon-owned table has `salon_id`; it comes from the signed-in person's salon
   membership (D-036), never from the request body; row-level security is the second guard (D-027). A customer's own data is
   scoped by their user id (D-025).
4. **Secrets only in environment variables**, never in the app, git, logs or screenshots.

## Baseline controls (built in Phase 0)

| Control | Where | Proven by |
|---|---|---|
| Config validated at startup; DB password never printed | `backend/.../config/AppConfig.kt` | `AppConfigTest` |
| HTTPS-only CORS origins in production | `AppConfig` | `AppConfigTest` |
| Request ID on every call, safe for logs | `plugins/Monitoring.kt` | `SecurityBaselineTest` |
| Access log without query strings/bodies (no phone numbers, OTPs, tokens) | `plugins/Monitoring.kt` | code review |
| Security headers (nosniff, no framing, no referrer, CSP, no-store); HSTS on staging/prod; no server version | `plugins/Security.kt` | `SecurityBaselineTest` |
| CORS allowlist only, no credentials | `plugins/Security.kt` | `SecurityBaselineTest` |
| Per-IP rate limit (429) | `plugins/Security.kt` | `SecurityBaselineTest` |
| 1 MB request body limit (413) | `plugins/Security.kt` | `SecurityBaselineTest` |
| Error envelope; no stack traces, exception messages or class names to clients | `plugins/ErrorHandling.kt` | `SecurityBaselineTest` |
| Health-check failures log exception type only | `health/DatabaseHealthCheck.kt` | code review |
| Login tokens checked on every protected route: Supabase public keys (ES256), issuer, audience, role, expiry; HS256/anon/expired/other-project tokens refused; tokens never logged | `auth/TokenVerifier.kt`, `auth/Authentication.kt` | `SupabaseTokenVerifierTest`, `MeRoutesTest` |
| Admin routes: login must be in `admins` (matched on Supabase user id, not email) **and** have passed the authenticator app (`aal2`), else 403 NOT_ADMIN / MFA_REQUIRED; every `/v1/admin` route is checked | `admins/AdminAccess.kt` | `AdminRoutesTest` |
| Supabase secret key only from the environment, never printed; the publishable key is refused in its place; Supabase error text (may contain emails) never logged | `config/AppConfig.kt`, `admins/AuthAdmin.kt` | `AppConfigTest`, `SupabaseAuthAdminTest` |
| Audit log is append-only (UPDATE / DELETE / TRUNCATE refused by the database); admin changes write their row in the same transaction | `V5`, `audit/AuditLog.kt` | `AdminTablesTest`, `AdminRepositoryTest` |
| Salon data by salon: the salon comes only from the signed-in person's membership (no salon id in any route, D-036); owner-only edits; one salon per person (D-035); staff can read, not edit | `salons/SalonService.kt`, `salons/SalonRoutes.kt` | `SalonRoutesTest` |
| Row-level security forced on salon tables; the backend's queries run as `glide_app` (not superuser, owner of nothing, no `BYPASSRLS`; Supabase's `postgres` login has it); salon and person set per transaction only (DF-31) | `V6`–`V8`, `db/DatabaseFactory.kt`, `db/RowSecurity.kt` | `AppRoleTest`, `SalonTablesTest`, `SalonMembersTablesTest` |
| Admin website: every page needs email code **and** authenticator app; logged-out → /login; idle 30 min / 12 h → logged out (DF-30), checked by the proxy on every request (prefetches too) **and** by `readSession()`; a refused login ends on /login with its cookie deleted, never in a redirect loop (WEB-008) | `admin/src/proxy.ts`, `auth/guard.ts`, `auth/cookies.ts`, `auth/require-admin.ts` | `guard.test.ts`, `proxy.test.ts`, `cookies.test.ts`, `require-admin.test.ts`, `app/pages.test.tsx` |
| Admin session: encrypted (AES-256-GCM) `__Host-` cookie, httpOnly + Secure + SameSite=strict; Supabase called only from the Next.js server, no tokens in browser JavaScript (DF-20) | `admin/src/auth/session.ts`, `auth/supabase-auth.ts` | `session.test.ts`, `actions.test.ts` |
| `flyway clean` disabled everywhere | `db/DatabaseFactory.kt` | `MigrationTest` |
| Edited migrations detected | Flyway validate | `MigrationTest` |
| Postgres bound to localhost in dev | `docker-compose.yml` | - |
| Container runs as non-root | `backend/Dockerfile` | BE-009 |
| Gradle wrapper pinned by SHA-256 | `gradle/wrapper/gradle-wrapper.properties` | Gradle itself |
| Secrets files git-ignored and denied to AI tools | `.gitignore`, `.claude/settings.json` | - |

Coming with their tasks: Android network security config and R8 (APP-001/002), admin security headers (WEB-001),
secret scanning + CodeQL + Dependabot (BE-012), auth + roles + tenant isolation (Phase 1).

## Known gaps (tracked)

| Gap | Why it's open | Task |
|---|---|---|
| Rate limit uses the direct client IP | behind a proxy all users share one IP; needs the proxy's forwarded-header config once hosting is known | BE-9xx (Pre-launch) |
| Admin login has no rate limit of its own: flooding it can use up Supabase's email allowance or get our server's IP briefly blocked at Supabase (codes stay unguessable) | not hosted yet; limits per client IP and per email need the host's forwarded-IP header | WEB-9xx (before hosting) |

## Secrets

| Secret | Lives in | Never in |
|---|---|---|
| DB passwords, Supabase service-role key, Razorpay key secret + webhook secret, WhatsApp token, R2 keys | `.env` locally (put there by the team); hosting provider env vars; GitHub Actions secrets for CI/CD | git, app, logs |
| Android signing keystore + passwords | GitHub Actions secrets (base64) + team password manager | git |
| `google-services.json` | GitHub Actions secret; local copy git-ignored | git |
| Test-user credentials | team password manager | git |

If a secret is ever committed: **rotate it immediately** (deleting the commit is not enough), then tell the team.

## Reporting a vulnerability

Tell the team lead privately (not in a public issue). Include the request ID if you have one.
