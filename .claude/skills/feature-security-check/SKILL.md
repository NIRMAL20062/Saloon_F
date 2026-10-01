---
name: feature-security-check
description: Security, data-integrity and flow review of one Glide task or feature before its PR (tenant isolation, authz, validation, secrets, payments/webhooks, logging, tests). Use at the end of every task, or when asked to security-check a feature.
---

# Feature security and integrity check

Scope: the diff of the current task (`git diff main...HEAD`) plus any code it calls. Report findings as a checklist with
`file:line`, then fix them (or list what needs the team). Baseline controls: `docs/SECURITY.md`.

## Access and tenancy
- [ ] Every new query on a business table filters by `salon_id` taken from the authenticated principal, never from input
- [ ] A test proves salon A gets 404 on salon B's resources (read **and** write)
- [ ] Every new route requires auth unless the task says it's public; role checks where roles differ
- [ ] IDs in URLs are not trusted: ownership checked before use

## Input and output
- [ ] Every input field validated (type, length, range, format) on the **backend**, not only in the app
- [ ] Errors use the error envelope; no stack traces, SQL, exception messages or internal IDs leak
- [ ] No raw server text shown in the Android UI or admin pages

## Secrets and data
- [ ] No keys, tokens or passwords in code, `BuildConfig`, resources, tests fixtures or logs
- [ ] Logs carry IDs only: no phone numbers, OTPs, tokens, payment details, request bodies
- [ ] New env vars validated in `AppConfig` and added to `.env.example` (placeholder values only)

## Payments and messaging (when touched)
- [ ] Razorpay order created server-side; amount computed server-side, never taken from the client
- [ ] Payment marked paid only after webhook signature verification (or server-side verify), not the app callback
- [ ] Webhooks verify signature **before** parsing and are idempotent (same event twice = one effect)
- [ ] WhatsApp/SMS sent only from the backend, only with approved templates

## Database integrity
- [ ] Constraints (NOT NULL, CHECK, UNIQUE per salon, FKs) enforce the rules, not just app code
- [ ] Multi-row changes run in one transaction
- [ ] Migration is new (no edits to merged ones) and applies on real Postgres

## Android specific
- [ ] No new cleartext traffic; tokens stored only in encrypted storage; no sensitive data in logs or screenshots of lists if the task says so
- [ ] Exported components (`android:exported`) justified

## Flow
- [ ] The task's end-to-end flow was run on the real stack and works, including the main failure path (backend down, validation error)

Finish with: **PASS** or **FIX NEEDED** plus the list.
