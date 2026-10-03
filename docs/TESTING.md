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
| whole system | browser → admin → backend → Postgres | Playwright + `docker compose --profile full` | `pnpm e2e` (arrives with WEB-004; until then CI runs a smoke check) |

**No database mocks.** Anything touching SQL runs on real Postgres 17 (same as production), so constraint, migration and
query bugs show up in tests, not in production.

## Required tests per endpoint (once auth and tenants exist)

- happy path: exact status and body, **and** the database state afterwards
- each validation rule → 400
- not found → 404
- **another salon's resource → 404** (tenant isolation, read and write)
- **another customer's data → 404** (customer-side endpoints, D-025)
- no token → 401 · wrong role → 403
- retry safety (same request/webhook twice → one effect) where retries can happen

## Real-data testing (D-008, D-011)

We test on **real services in test mode** from the start, but **without hosting** until the Pre-launch phase:

| Phase | Backend + database | Phones | External services |
|---|---|---|---|
| **Development (now)** | on the developer's laptop (`docker compose`) | team phones over USB (`adb reverse tcp:8080 tcp:8080`) | real services in **test mode**; webhooks (Razorpay, WhatsApp) reach the laptop through a free tunnel |
| **Pre-launch** | free-tier hosting (provider picked then, D-010) = **staging** | Firebase App Distribution builds | test mode |
| **Production** | hosted | Play Store | live keys |

| Service | Test-mode setup |
|---|---|
| PostgreSQL | local Docker now; a separate free hosted database for staging later, never shared with production |
| Razorpay | **test mode** keys: real API, real webhooks, no real money. Test cards/UPI from Razorpay docs |
| Phone/email login | **Supabase Auth test phone numbers** with fixed OTP codes (free, no SMS sent); team phones with real SMS through a Twilio trial account (D-016) |
| WhatsApp Cloud API | Meta **test number** and test recipients |

### Test users for every user type (D-012)

A seed script (BE-021) creates the same named test users every time, in **two** test salons that are already verified (live), so tenant isolation
can also be checked by hand (log in as Salon A's owner and try to see Salon B's data: you must not).

| User type | Test Salon A | Test Salon B | Can do (to be confirmed with the feature list) |
|---|---|---|---|
| Salon owner | `owner.a` | `owner.b` | everything in their salon: profile, bank details, staff, services, prices, bookings, bills, reports, subscription |
| Manager | `manager.a` | `manager.b` | everything the owner can, except bank details and the subscription (D-034, D-037) |
| Staff | `staff.a` | `staff.b` | see and accept their own bookings only (D-034) |
| Customer (customer side) | `customer.a` | `customer.b` | find salons, book, pay, cancel, review, raise disputes (D-015) |
| Internal admin (our team) | `admin` | (all salons) | admin panel only |

Logins (test phone numbers / emails and codes) live in the team's password manager, **not in git**. Test data is fake; real customer data never goes into development or staging.

## Writing good tests (house style)

- Name tests as sentences: `` `other salon's booking returns 404` ``.
- Arrange / act / assert, separated by blank lines.
- One behaviour per test. Assert on exact values, not just "not null".
- Tests must pass in any order and in parallel; create your own data, don't depend on another test's rows.
- A flaky test is a bug: fix it or open a task. Never `@Disabled` it silently.
