# Security

Per-feature checklist: run `/feature-security-check` (see `.claude/skills/feature-security-check/SKILL.md`) before every PR.

## Product rules (from the plan, non-negotiable)

1. The app **never** calls WhatsApp directly; only the backend does.
2. Razorpay orders are **created** on the backend (amount computed server-side) and payments are **confirmed only by a
   verified webhook signature** (or server-side verification), never by the app's success callback alone.
3. **Multi-tenant from day 1**: every business table has `salon_id`; it comes from the authenticated user, never from input.
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
| Rate limit uses the direct client IP | behind a proxy all users share one IP; needs the proxy's forwarded-header config once hosting is known | Q-004 |
| Login exists on the backend (BE-016); roles and salon membership checks come next | Phase 1 | BE-017, BE-020 |
| Admin panel must not be public until admin login exists | Phase 1 | WEB-1xx (Q-005) |

## Secrets

| Secret | Lives in | Never in |
|---|---|---|
| DB password, JWT signing key, Razorpay key secret + webhook secret, WhatsApp token, R2 keys | hosting provider env vars; GitHub Actions secrets for CI/CD | git, app, logs |
| Android signing keystore + passwords | GitHub Actions secrets (base64) + team password manager | git |
| `google-services.json` | GitHub Actions secret; local copy git-ignored | git |
| Test-user credentials | team password manager | git |

If a secret is ever committed: **rotate it immediately** (deleting the commit is not enough), then tell the team.

## Reporting a vulnerability

Tell the team lead privately (not in a public issue). Include the request ID if you have one.
