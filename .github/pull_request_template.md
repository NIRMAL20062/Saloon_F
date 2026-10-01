## Task

<!-- One task per PR. Title: "<ID>: <task title>" -->
Task: `APP-/WEB-/BE-0XX` · Link: tasks/<FILE>.md

## What changed

<!-- 2–5 bullets. Reviewers read the commits for details. -->

## Done when (copy from the task and tick)

- [ ] ...

## Definition of Done

- [ ] Tests added for every new behaviour (unit + integration on real Postgres where the DB is touched + UI states)
- [ ] `/feature-security-check` run; findings fixed (tenant isolation, validation, no secrets in code or logs)
- [ ] Database changes only via a **new** migration; constraints and `salon_id` in place
- [ ] Flow run end-to-end on the real stack (say what you checked)
- [ ] `./gradlew check` green · `pnpm verify` green (if `admin/` changed)
- [ ] Docs / OpenAPI spec updated if behaviour or API changed
- [ ] Task moved to the `_COMPLETED.md` file with commit hashes
