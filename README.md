# Glide

> Working name. Salon marketplace for India: **one Android app** (customer side + salon side, picked at first login), a Kotlin/Ktor backend, and an internal admin panel.

| Folder | What it is | Stack |
|---|---|---|
| [`android/`](android/) | The one app: customers book salons; owners, managers and staff run their salon (D-023, D-034) | Kotlin, Jetpack Compose, Hilt, Retrofit |
| [`backend/`](backend/) | API server | Kotlin, Ktor, Exposed, PostgreSQL, Flyway |
| [`shared/`](shared/) | API contract (request/response models) used by Android **and** backend | Kotlin, kotlinx.serialization |
| [`admin/`](admin/) | Internal admin panel for our team | Next.js (TypeScript) |
| [`docs/`](docs/) | Architecture, roadmap, feature specs, security, testing | Markdown |

## Quick start

Prerequisites: JDK 21+, Docker, Android Studio (for `android/`), Node 20+ with pnpm via `corepack enable` (for `admin/`).

```bash
cp .env.example .env                 # local defaults only
docker compose up -d postgres        # or: docker-compose up -d postgres
./gradlew :backend:run               # API on http://localhost:8080/health
./gradlew :android:app:installDebug  # app on a USB phone or emulator
adb reverse tcp:8080 tcp:8080         # lets the app reach the backend on this laptop
cd admin && cp .env.example .env.local && pnpm install && pnpm dev   # admin on http://localhost:3000
```

Full checks, same as CI: `./gradlew check` and `cd admin && pnpm verify`.

## Where to read next

- **Product plan:** [Salon_App_Task_Wise_Development.md](Salon_App_Task_Wise_Development.md) (salon side, draft for review) + [ChatGPT.md](ChatGPT.md) (spec v2: customer side, payments, disputes)
- **[tasks/](tasks/README.md): what to build next and what's done, for the App, Web and Backend. All work starts here.**
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): how the pieces fit, and how Android + web are built together
- [docs/DEVELOPMENT_WORKFLOW.md](docs/DEVELOPMENT_WORKFLOW.md): task → branch → small commits → PR → CI → deploy
- [docs/TESTING.md](docs/TESTING.md) · [docs/SECURITY.md](docs/SECURITY.md) · [docs/DATABASE.md](docs/DATABASE.md) · [docs/DECISIONS.md](docs/DECISIONS.md)
- [CLAUDE.md](CLAUDE.md): rules for AI coding tools (and humans)
