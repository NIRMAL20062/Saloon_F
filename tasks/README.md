# Tasks

**All work is driven by these files.** Nothing gets built unless it is a task here first.

| Area | To do | Done |
|---|---|---|
| 📱 Android app (one app: customer side + salon side) | [APP_TASKS.md](APP_TASKS.md) | [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md) |
| 🖥️ Web admin panel | [WEB_TASKS.md](WEB_TASKS.md) | [WEB_TASKS_COMPLETED.md](WEB_TASKS_COMPLETED.md) |
| ⚙️ Backend (API + database) and Platform (build, CI/CD, docs) | [BACKEND_TASKS.md](BACKEND_TASKS.md) | [BACKEND_TASKS_COMPLETED.md](BACKEND_TASKS_COMPLETED.md) |

## Build order

What gets built next, across all three files. Each step is one task = one pull request, checked by the team before the next.

| # | Task | What you'll see | Needs from team first |
|---|---|---|---|
| 1 | BE-016 | backend accepts Supabase logins (`/v1/me`), remembers customer/salon choice | **Supabase project + phone login with test numbers** |
| 2 | APP-004 | the app: phone → OTP → logged in, stays logged in, logout; already in the new polished look (D-031) | (same Supabase project) |
| 3 | APP-011 | the rest of the design system (cards, chips, sheets, empty states) | optional: apps you like, brand colour |
| 4 | BE-018 + APP-005 | first login asks "customer or salon?" (final, D-030); customers enter name/email | nothing |
| 5 | WEB-002 → BE-020 → WEB-005 | admin website: email code + authenticator login | first admin's email; Supabase email + MFA on; service-role key |
| 6 | BE-017 + APP-006 | owner creates a salon + bank details → "under verification" | encryption key in `.env` |
| 7 | BE-019 → BE-022 + WEB-007 | audit log; admins verify salons → the salon goes live (D-033) | nothing |
| 8 | APP-007, APP-008 | customer side tabs; salon home + menu by role | nothing |
| 9 | APP-012 | owner/manager add staff by phone; staff log in straight to "My bookings" (D-034) | nothing |
| 10 | WEB-006 | admins invite admins | nothing |
| 11 | APP-009 | customer side: location or city | launch city |
| 12 | BE-021 | test users for every type | nothing |
| 13 | APP-010 | crash reports in Firebase | `google-services.json` |

Then Phase 2 (salon setup), Phase 3 (appointments), Phase 4 (customer booking)… as outlined in each file.
Platform backlog items (security scanning, CD) are done in between when useful; they never block features.

## Task IDs and status

- IDs: `APP-001`, `WEB-001`, `BE-001`. Never reused, never renumbered. Outline items use placeholders
  (`APP-1xx` = Phase 2, `APP-2xx` = Phase 3 … `APP-6xx` = Phase 7, `APP-9xx` = Pre-launch; same for `BE-` and `WEB-`)
  and get a real ID when they're written out in full.
- Phases: **0** walking skeleton · **1** foundations (tenants, login, roles) · **2+** product features (from the team's plan) ·
  **Pre-launch** hosting, monitoring, backups, store release (D-011: nothing is hosted before this)
- **Needs from team** on a task = something only the team can do (create an account, provide a key or file, decide).
  The AI asks for it when the task starts.
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
4. **PR.** Push the branch and open a pull request (link printed by `git push`); tick the task's "Done when" list.
   CI must be green. **The team merges it**; nobody pushes to `main` directly (D-021).
5. **Test on the phone and report.** Install the build on the team phone and run the task's flow, then tell the team what was
   built, how to test it themselves, and what is needed from them (keys, accounts, answers). **The next task starts only after their OK.**
6. **Close.** After the team's OK, as the **last commit of the same PR** (before it's merged, DF-21): **move** the whole task
   block to the `_COMPLETED` file and add the date, commit hashes, tests added, and security/database notes; update
   "Where we are" in the root `CLAUDE.md`. Then the team merges.

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
- **Needs from team:** accounts, keys, files, decisions or manual steps (or "nothing")
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
