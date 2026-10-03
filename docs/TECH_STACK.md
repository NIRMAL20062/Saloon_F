# Tech Stack

> The stack as decided. Changes go through [DECISIONS.md](DECISIONS.md) first. What we build with it: [PRODUCT.md](PRODUCT.md).

## Android app (one app, customer side + salon side)

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3, our own design system in `ui/theme` + `ui/components` (D-031) |
| Architecture | MVVM, layers UI → ViewModel → UseCase → Repository |
| DI | Hilt |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Login | Supabase Auth over its REST API: phone + SMS code, session encrypted on the phone (D-016, DF-19) |
| Local DB | Room, offline cache only: no internet = view only (D-019) |
| Networking | Retrofit + OkHttp + Kotlin Serialization, models from `:shared` |
| Background work | WorkManager (retries, background refresh) |
| Images | Coil |
| Push notifications | Firebase Cloud Messaging |
| Crash and analytics | Firebase Crashlytics + Analytics (to prove design-partner usage) |
| Payments SDK | Razorpay Android SDK (Standard Checkout); orders are always created by the backend |
| Min SDK | 24 (Android 7.0), target latest stable (37) |

## Backend

| Layer | Choice |
|---|---|
| Language/framework | Kotlin + Ktor (same language as the app) |
| Database | PostgreSQL 17, migrations with Flyway |
| ORM | Exposed (D-003) |
| Auth | The backend verifies Supabase's tokens (public keys); it never issues its own (D-016) |
| Jobs/scheduler | Quartz or a simple cron worker (reminders, reconciliation, slot-hold expiry) |
| Hosting | free tier, picked at Pre-launch; nothing hosted during development (D-010, D-011) |
| File storage | Cloudflare R2 or S3 (invoice PDFs, photos) |
| Secrets | Environment variables only, never in the app or git |

Supabase is used **only for login**; the database is our own PostgreSQL (local Docker during development).

## Integrations

| Need | Service |
|---|---|
| Customer payments | Razorpay with **Route**: every online payment (in the app, or link / UPI QR at the salon) goes through our account; our fee stays, the salon's share goes to its linked account. Online only, no cash (D-028, D-029) |
| Salon subscription (our revenue) | Razorpay Subscriptions, one plan for now, price editable by admins (D-032) |
| WhatsApp messages | WhatsApp Business Cloud API (Meta), approved templates, sent only by the backend |
| SMS | Twilio, inside Supabase, for login codes only (D-016) |
| Invoices | Server-side PDF generation (OpenHTMLToPDF or iText) |

## Web: internal admin website (D-002)

| Layer | Choice |
|---|---|
| Purpose | For our team only (not salon-facing, not customer-facing) |
| Framework | Next.js (App Router) + TypeScript + Tailwind CSS |
| Login | Email code + authenticator app through Supabase, on the Next.js server only (DF-16, DF-20) |
| API types | Generated from `docs/api/openapi.yaml` with `openapi-typescript`, never hand-copied |
| Package manager | pnpm |

## Tooling

- GitHub monorepo (`/android`, `/backend`, `/shared`, `/admin`, `/docs`, `/tasks`), GitHub Actions CI (`ci-ok`)
- Firebase App Distribution to ship builds to design partners (Pre-launch)
- AI coding tools (Claude Code) following `CLAUDE.md` and `.claude/skills/`

## Architecture overview

```
Android app (Compose)  <──HTTPS/JSON──>  Ktor backend  <──>  PostgreSQL
   │  Room (offline, view only)             │
   │  Supabase Auth (login)                 ├── Razorpay Route (orders, payouts, refunds, subscriptions, webhooks)
   └── Razorpay Checkout SDK                ├── WhatsApp Cloud API (templates, webhooks)
                                            ├── FCM (push)
Admin website (Next.js) <──HTTPS/JSON──┘    └── Scheduler (reminders, reconciliation)
```

### Non-negotiable rules

1. **The app never calls WhatsApp directly.** Always via the backend.
2. **Razorpay orders are created and payments verified on the backend.** Verify the webhook signature, never only the app's success callback.
3. **Multi-tenant from day 1:** every salon-owned table has `salon_id` (D-025).
4. **Secrets live in environment variables only, never in the app or in git.**
