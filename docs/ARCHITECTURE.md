# Architecture

## The pieces

```
┌──────────────────────┐                         ┌──────────────────────────────┐
│ Android app          │  HTTPS / JSON (/v1/...) │ Ktor backend                 │      ┌────────────┐
│ Compose · Hilt       │ ──────────────────────▶ │ routes → services → repos    │ ───▶ │ PostgreSQL │
│ Room (offline)       │                         │ Flyway migrations            │      └────────────┘
│ Razorpay Checkout SDK│                         │                              │
└──────────────────────┘                         │  ├─ Razorpay (orders, subs,  │
                                                 │  │   webhooks, verified)     │
┌──────────────────────┐  HTTPS / JSON           │  ├─ WhatsApp Cloud API       │
│ Admin panel (Next.js)│ ──────────────────────▶ │  ├─ FCM (push)               │
│ internal, our team   │  (server-side fetch)    │  ├─ R2/S3 (invoice PDFs)     │
└──────────────────────┘                         │  └─ Scheduler (reminders)    │
                                                 └──────────────────────────────┘
```

Rules (from the product plan): the app never calls WhatsApp directly; Razorpay is verified on the backend; every table has `salon_id`; secrets live only in environment variables.

## Monorepo

```
shared/    Kotlin API contract (DTOs, routes, error codes)   ← compiled into backend AND android
backend/   Ktor server, Exposed, Flyway SQL migrations
android/   Android app (module :android:app)
admin/     Next.js admin panel (pnpm, separate from Gradle)
docs/      decisions, architecture, API spec (docs/api/openapi.yaml)
tasks/     what to build next / what's done, per area
```

One root Gradle build holds `shared`, `backend` and `android`, so they always use the same Kotlin version and the same contract. `admin/` is a separate Node project.

## How the Android app and the web panel are built at the same time

Both clients sit on **one backend and one API contract**, so work never has to be done twice and the two never drift:

1. **One contract, two outputs.**
   - Android compiles against the Kotlin DTOs in `shared/`. Change a DTO and the backend **and** the app stop compiling until both match.
   - The admin panel (TypeScript) can't use Kotlin, so it uses TypeScript types **generated** from `docs/api/openapi.yaml`.
     A backend contract test checks that real responses match the spec, so the spec can't lie.
2. **Backend first, then both clients in parallel.** Each feature is split into tasks: `BE-` (contract + DB + API) first, then
   `APP-` and `WEB-`, which depend only on the `BE-` task and can be built **at the same time** by different people (or AI sessions).
3. **CI runs only what changed.** A PR touching `admin/` doesn't wait for the Android build, and vice versa. A contract change in
   `shared/` runs everything.
4. **End-to-end tests run on the real stack** (browser → admin → backend → Postgres), so "works on my machine" can't sneak in.

## Inside the backend

| Layer | Does | Doesn't |
|---|---|---|
| Route (`<Feature>Routes.kt`) | parse, validate shape, call service, respond with a `shared` DTO | business rules, SQL |
| Service | business rules, transactions, calls to Razorpay/WhatsApp through interfaces | HTTP details |
| Repository | Exposed SQL, always scoped by `salon_id` | decisions |

Cross-cutting plugins (`backend/.../plugins/`): request IDs, access logs, security headers, CORS, rate limit, body limit, error envelope.
Everything external is injected through `AppDependencies` so tests can swap in fakes.

## Inside the Android app

`Composable screen → ViewModel (StateFlow<UiState>) → UseCase → Repository → Retrofit (shared DTOs) / Room`.
Hilt wires the graph. Debug builds call `http://127.0.0.1:8080`, forwarded to the laptop's backend by `adb reverse tcp:8080 tcp:8080` (USB phone or emulator); release builds are HTTPS-only.

## Environments

| | Local | Staging | Production |
|---|---|---|---|
| Purpose | development | real-data testing by the team and design partners | real salons |
| Database | Docker Postgres | hosted Postgres (provider: Q-002/Q-004) | hosted Postgres |
| Razorpay | test keys | **test mode** | live keys |
| Firebase Auth | test phone numbers | test phone numbers + real team phones | real |
| WhatsApp | test number | test number | approved business number |
| Users | none / seeded | **seeded test users for every user type** (Q-003) | real |
| Android build | debug | staging build via Firebase App Distribution | Play Store |

Hosting is decided later (Q-004). CD already produces the deployable artifacts.

## Request lifecycle (backend)

1. `X-Request-Id` assigned (caller's if safe, else generated), echoed in the response, put in every log line.
2. Rate limit, then body-size limit.
3. Route → service → repository → Postgres.
4. Any failure → `{"error":{"code","message","requestId"}}`. Details go to the server log only.
