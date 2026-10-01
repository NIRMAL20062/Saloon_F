# Tasks

**All work is driven by these files.** Nothing gets built unless it is a task here first.

| Area | To do | Done |
|---|---|---|
| 📱 Android app | [APP_TASKS.md](APP_TASKS.md) | [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md) |
| 🖥️ Web admin panel | [WEB_TASKS.md](WEB_TASKS.md) | [WEB_TASKS_COMPLETED.md](WEB_TASKS_COMPLETED.md) |
| ⚙️ Backend (API + database) and Platform (build, CI/CD, docs) | [BACKEND_TASKS.md](BACKEND_TASKS.md) | [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md) |

## Task IDs and status

- IDs: `APP-001`, `WEB-001`, `BE-001`. Never reused, never renumbered.
- Status: ⬜ To do · 🔄 In progress · ⛔ Blocked (say on what) · ✅ Done (moved to the `_COMPLETED` file)
- A feature that touches several areas is split into one task per area, linked through **Depends on**.
  Example: "Book appointment" = `BE-0xx` (API + table) → `APP-0xx` (screen) and `WEB-0xx` (admin view).

## How a task flows

1. **Add.** The team writes the feature as task(s) using the template below, in the right file(s).
   If anything is unclear, the developer or AI **asks**; it never guesses.
2. **Pick.** Take the top ⬜ task whose dependencies are all ✅. Mark it 🔄 with your name.
3. **Build** on a branch `task/<ID>-short-name`, in **small commits**, each one a single unit of work
   that builds and passes tests on its own. Every commit message ends with the task ID, e.g.
   `feat(android): show backend status on home screen (APP-003)`.
4. **PR.** Open a pull request and tick the task's "Done when" list. CI must be green.
5. **Report and wait.** Tell the team what was built, how to test it on the phone/browser step by step,
   and what is needed from them (keys, accounts, answers). **The next task starts only after their OK.**
6. **Close.** After the team's OK (and merge), **move** the whole task block to the `_COMPLETED` file and add the date,
   commit hashes, tests added, and security/database notes.

## Definition of Done (every task)

- [ ] Every "Done when" item is ticked
- [ ] **Tests**: unit tests for logic; integration tests for API + real database; UI tests for screens
- [ ] **Security**: input validated, authorization checked (once auth exists), no secrets in code or logs,
      tenant isolation (`salon_id`) tested once tenants exist. See [docs/SECURITY.md](../docs/SECURITY.md)
- [ ] **Database**: changes only through a new Flyway migration; constraints, indexes and `salon_id` in place.
      See [docs/DATABASE.md](../docs/DATABASE.md)
- [ ] **Flow**: the user flow works end-to-end on the real backend, not only with fakes
- [ ] `./gradlew check` and `cd admin && pnpm verify` pass locally, CI is green
- [ ] Docs updated if behaviour, API or setup changed

## Task template

Copy this into the right `_TASKS.md` file:

```markdown
### APP-0XX · Short title
- **Phase:** N · **Status:** ⬜ To do · **Owner:** - · **Depends on:** BE-0XX
- **Why:** one line on the user or business need
- **Scope:**
  - what is included
  - what is explicitly NOT included
- **Done when:**
  - [ ] behaviour 1 (observable, testable)
  - [ ] Tests: which unit / integration / UI tests
  - [ ] Security: what must be checked
  - [ ] Database: migration, constraints, salon_id (or "none")
  - [ ] Flow: the end-to-end path that must work
```
