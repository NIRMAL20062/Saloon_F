---
name: work-task
description: Pick up and complete one task from tasks/ (APP-, WEB- or BE- ID) end to end, following the Definition of Done. Use when asked to "do the next task", "work on APP-003", or continue development.
---

# Work a task

Argument: a task ID (e.g. `APP-003`), or nothing for "the next one".

## 1. Pick

1. Read `tasks/README.md`, then the relevant `tasks/*_TASKS.md` file(s).
2. With an ID: find that task. Without one: take the **first ⬜ task whose dependencies are all ✅** (check the
   `_COMPLETED.md` files). If several areas have candidates, ask which one.
3. If the task is unclear, has unanswered open questions (see `docs/DECISIONS.md` § Open questions), or needs information
   nobody gave (fields, roles, copy, provider, prices): **stop and ask**. List the exact questions.
4. Mark it `🔄 In progress · **Owner:** <you>` in the task file.

## 2. Plan

Write a short plan in chat: files to touch, the commits you'll make (each one a buildable, tested unit), and which test
proves each "Done when" item. Follow the vertical-slice order in the root `CLAUDE.md`.

## 3. Build

For each planned unit:
1. Write code **and its tests** together.
2. `./gradlew spotlessApply` then `./gradlew check` (+ `cd admin && pnpm verify` if `admin/` changed). Must be green.
3. Commit only that unit: `type(scope): summary (TASK-ID)`.

Use `/add-endpoint` for API work and `/db-migration` for schema changes.

## 4. Verify the flow

Run the real flow, not just tests: `docker compose up -d postgres`, `./gradlew :backend:run`, then the app (emulator) or
admin (`pnpm dev`). Confirm every "Done when → Flow" item by hand and say what you saw.

## 5. Security and integrity

Run `/feature-security-check` on the task's changes. Fix anything it finds before closing.

## 6. Close

1. Tick every box in the task's "Done when" list. If any can't be ticked, the task isn't done; say why.
2. **Move** the task block from `*_TASKS.md` to the matching `*_COMPLETED.md` (bottom), and add:
   `**Completed:** <date> · **Commits:** <hashes>`, what was built, tests added (class + count), security notes, database notes.
3. Commit the task-file update: `docs(tasks): complete <ID>`.
4. Report: what was built, test counts, anything deferred, and the next available task.
