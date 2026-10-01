# Testing

Two promises: **every task ships with tests**, and **we test on real services from the start** (D-008), not only on mocks.

## What each area tests

| Area | Level | Tools | Runs on |
|---|---|---|---|
| `shared` | wire format of every DTO (exact JSON) | JUnit 5 | `./gradlew :shared:test` |
| `backend` | unit: config, services | JUnit 5 | `./gradlew :backend:test` |
| `backend` | integration: routes + **real PostgreSQL** | Ktor `testApplication` + Testcontainers | same (needs Docker) |
| `backend` | contract: responses match `docs/api/openapi.yaml` | OpenAPI validator | same |
| `backend` | security controls (one test per control) | `SecurityBaselineTest` | same |
| `android` | ViewModel / use case logic, every UI state | JUnit 4 + coroutines-test | `./gradlew :android:app:testDebugUnitTest` |
| `android` | network layer against real Retrofit + `ApiJson` | MockWebServer | same |
| `android` | screens, every state | Compose UI test on Robolectric | same |
| `admin` | components, API client | Vitest + Testing Library | `pnpm test` |
| whole system | browser → admin → backend → Postgres | Playwright + `docker compose --profile full` | `pnpm e2e` |

**No database mocks.** Anything touching SQL runs on real Postgres 17 (same as production), so constraint, migration and
query bugs show up in tests, not in production.

## Required tests per endpoint (once auth and tenants exist)

- happy path: exact status and body, **and** the database state afterwards
- each validation rule → 400
- not found → 404
- **another salon's resource → 404** (tenant isolation, read and write)
- no token → 401 · wrong role → 403
- retry safety (same request/webhook twice → one effect) where retries can happen

## Real-data testing: staging (D-008)

Staging is a full copy of production running on **real services in test mode**:

| Service | Staging setup |
|---|---|
| PostgreSQL | real hosted database, never shared with production (provider: Q-002/Q-004) |
| Razorpay | **test mode** keys: real API, real webhooks, no real money. Test cards/UPI from Razorpay docs |
| Firebase Auth (phone OTP) | Firebase **test phone numbers** with fixed codes, plus team members' real phones |
| WhatsApp Cloud API | Meta **test number** and test recipients |
| FCM | real pushes to team devices |

### Test users for every user type

A seed script (Phase 1 task `BE-1xx`) creates the same named test users in staging every time:
one per user type, in at least **two test salons** (so tenant isolation can be checked by hand too).
The user types themselves are **open question Q-003**. The list will be filled in here once the team confirms it.

| User type | Salon | Login (test phone) | Notes |
|---|---|---|---|
| _waiting for Q-003_ | Test Salon A | | |
| | Test Salon B | | |

Rules: test credentials live in the team's password manager, not in git. Staging never holds real customer data.

## Writing good tests (house style)

- Name tests as sentences: `` `other salon's booking returns 404` ``.
- Arrange / act / assert, separated by blank lines.
- One behaviour per test. Assert on exact values, not just "not null".
- Tests must pass in any order and in parallel; create your own data, don't depend on another test's rows.
- A flaky test is a bug: fix it or open a task. Never `@Disabled` it silently.
