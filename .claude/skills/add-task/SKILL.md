---
name: add-task
description: Turn a feature or change described by the team into properly written tasks in tasks/APP_TASKS.md, WEB_TASKS.md and/or BACKEND_TASKS.md. Use when the team describes a new feature, screen, integration or change.
---

# Add a task

Input: the team's description of a feature or change.

## 1. Understand, then ask

Restate the feature in 2–3 lines. Then list **every** missing detail as numbered questions and wait for answers. Typical gaps:
- Who uses it? (which user type/role) Who must NOT be able to?
- Which data, with which fields? Required or optional? Limits (length, ranges)?
- Android, admin panel, or both?
- External services involved (Razorpay, WhatsApp, FCM)? Test-mode credentials available?
- Offline behaviour on Android? Notifications?
- What exactly counts as done?

Never fill these in yourself. If the team says "you decide" for something small, record it in `docs/DECISIONS.md` § Implementation defaults.

## 2. Split by area

One task per area, linked by **Depends on**:
- `BE-` : contract (shared DTOs + openapi), migration, API, backend tests
- `APP-`: Android screens/logic using that API
- `WEB-`: admin panel pages using that API

Keep each task small enough for one PR (roughly ≤ 1–2 days). Split bigger ones.

## 3. Write

Use the template in `tasks/README.md`. Next free ID = highest ID in **both** the `_TASKS` and `_COMPLETED` file + 1.
Every task's "Done when" must include concrete **Tests**, **Security** (authz, `salon_id` isolation, validation),
**Database** (migration, constraints, indexes, or "none") and **Flow** items.

Add the tasks under `## Features: added by the team` (or the phase the team names).

## 4. Confirm

Show the team the new tasks and ask for approval before anyone starts building. Commit with `docs(tasks): add <IDs> for <feature>`.
