# Tech Stack

> Source: the team's product plan, sections 4–5 (copied verbatim below). Changes to the stack go through [DECISIONS.md](DECISIONS.md) first.

## Android app

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean-ish layers (UI → ViewModel → UseCase → Repository) |
| DI | Hilt |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Local DB | Room (offline cache and drafts) |
| Networking | Retrofit + OkHttp + Kotlin Serialization |
| Background work | WorkManager (sync, retry) |
| Images | Coil |
| Push notifications | Firebase Cloud Messaging |
| Crash and analytics | Firebase Crashlytics + Analytics (needed to prove design partner usage) |
| Payments SDK | Razorpay Android SDK (Standard Checkout) |
| Min SDK | 24 (Android 7.0), target latest stable |

## Backend

| Layer | Choice |
|---|---|
| Language/framework | Kotlin + Ktor (same language as the app, easier for a small team) |
| Database | PostgreSQL |
| ORM | ~~Exposed or Ktorm~~ **Exposed** (decided, see D-003) |
| Auth | Phone OTP (Firebase Auth) → backend issues JWT |
| Jobs/scheduler | Quartz or a simple cron worker for reminders |
| Hosting | Railway / Render / a small VPS (start cheap): **not decided yet** |
| File storage | Cloudflare R2 or S3 (invoice PDFs) |
| Secrets | Environment variables only, never in the app |

> Faster alternative: Supabase (Postgres + Auth + Edge Functions) cuts backend time a lot. Pick it if your team is weak on backend. Decide in week 1 and stick with it.
> **Decided: Ktor + Exposed (D-003).** Supabase may still be used as a *hosted Postgres* provider (open question Q-002).

## Integrations

| Need | Service |
|---|---|
| Customer payments (salon collects from clients) | Razorpay Payments: Payment Links / UPI QR / Standard Checkout |
| Billing the salon (our subscription revenue) | Razorpay Subscriptions |
| WhatsApp messages | WhatsApp Business Cloud API (Meta) directly, or a BSP such as Gupshup / Interakt / AiSensy to start faster |
| SMS fallback | MSG91 or Twilio (optional) |
| Invoices | Server-side PDF generation (OpenHTMLToPDF or iText) |

## Tooling

- GitHub monorepo, GitHub Actions for CI. Layout: `/android`, `/backend`, `/docs`, plus `/shared` (D-004) and `/admin` (D-002)
- Firebase App Distribution to ship builds to design partners
- Postman for API tests, Figma for screens
- AI coding tools (Claude Code etc.) with a shared `CLAUDE.md` describing architecture and conventions

## Web: internal admin panel (added, D-002)

| Layer | Choice |
|---|---|
| Purpose | Internal admin panel for our team only (not salon-facing, not customer-facing) |
| Framework | Next.js (App Router) + TypeScript + Tailwind CSS |
| API types | Generated from `docs/api/openapi.yaml` with `openapi-typescript`, so nothing is hand-copied |
| Package manager | pnpm |

## Architecture overview

```
Android App (Compose)  <──HTTPS/JSON──>  Ktor Backend  <──>  PostgreSQL
   │  Room (offline)                        │
   │                                        ├── Razorpay (orders, subscriptions, webhooks)
   └── Razorpay Checkout SDK                ├── WhatsApp Cloud API (templates, webhooks)
                                            ├── FCM (push)
Admin Panel (Next.js) <──HTTPS/JSON──┘      └── Scheduler (reminders, daily summary)
```

### Non-negotiable rules

1. **The app never calls WhatsApp directly.** Always via backend.
2. **Razorpay order creation and payment verification happen on the backend.** Verify via webhook signature, not just the app's success callback.
3. **Multi-tenant from day 1: every table has `salon_id`.**
4. **Secrets live in environment variables only, never in the app or in git.**
