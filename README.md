# Glide

> Working name. Salon management platform: Android app for salons, Kotlin/Ktor backend, and an internal admin panel.

| Folder | What it is | Stack |
|---|---|---|
| [`android/`](android/) | Salon owner/staff app | Kotlin, Jetpack Compose, Hilt, Retrofit |
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
./gradlew :android:app:installDebug  # app on a running emulator
cd admin && pnpm install && pnpm dev # admin on http://localhost:3000
```

Full checks, same as CI: `./gradlew check` and `cd admin && pnpm verify`.

## Where to read next

- **[tasks/](tasks/README.md): what to build next and what's done, for the App, Web and Backend. All work starts here.**
- [docs/ROADMAP.md](docs/ROADMAP.md): phases and tasks, and what's being built now
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): how the pieces fit, and how Android + web are built together
- [docs/DEVELOPMENT_WORKFLOW.md](docs/DEVELOPMENT_WORKFLOW.md): task → branch → small commits → PR → CI → deploy
- [CLAUDE.md](CLAUDE.md): rules for AI coding tools (and humans)
