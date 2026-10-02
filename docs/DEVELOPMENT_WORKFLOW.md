# Development Workflow

Everything starts from a task in [`tasks/`](../tasks/README.md). No task, no code.

## First-time setup

| Tool | Version | Used for |
|---|---|---|
| JDK | 21+ (Android Studio's bundled JDK works) | backend, shared, Android builds |
| Docker | any recent | local Postgres, Testcontainers in tests |
| Android Studio | 2026.1.4+ (AGP 9.4) | Android app; open the **repo root** |
| Node.js + pnpm | Node 20+, then `corepack enable` | admin panel |

```bash
git clone <repo> && cd <repo>
cp .env.example .env                 # local values only
docker compose up -d postgres
./gradlew check                      # should be green before you change anything
```

## The loop: task → branch → small commits → PR → merge

```
tasks/*_TASKS.md ──▶ git switch -c task/APP-003-status-screen
                     ├─ commit: feat(android): add status repository (APP-003)
                     ├─ commit: feat(android): add status view model (APP-003)
                     ├─ commit: feat(android): add status screen (APP-003)
                     └─ commit: docs(tasks): complete APP-003
                 ──▶ push, open PR ──▶ CI green + 1 review ──▶ merge commit (keeps the small commits)
```

1. **Pick** a ⬜ task whose dependencies are ✅.
2. **Branch** from `main`: `task/<ID>-short-name`. The first commit marks the task 🔄 with your name; push the branch right
   away so the team can see it's taken (`main` is protected, so the mark lives on the branch until the merge).
3. **Small commits.** Each commit is one unit of work, builds, and passes `./gradlew check` on its own. Message:
   `type(scope): what changed (TASK-ID)`. Small commits make `git bisect` and reverts painless.
4. **PR**: title `<ID>: <task title>`; the template asks you to tick the task's "Done when" list.
5. **CI must be green.** Never merge red. Never skip or delete a test to get green.
6. **Close the task** after the team's OK, as the PR's last commit: move it to the `_COMPLETED.md` file with commit hashes and
   update "Where we are" in `CLAUDE.md` (DF-20; the `/work-task` skill does this).
7. **Merge with a merge commit** (not squash) so the small commits stay visible in history.

### Branch protection on `main` (set in GitHub → Settings → Branches)

- Require a pull request, with 1 approval
- Require status check **`ci-ok`** to pass
- No force pushes, no deletions

## Commit types

`feat` new behaviour · `fix` bug fix · `test` tests only · `refactor` no behaviour change · `docs` docs/tasks ·
`build` Gradle/deps/Docker · `ci` workflows · `chore` anything else. Scopes: `android`, `backend`, `shared`, `admin`, `ci`, `docs`, `tasks`.

## Environments and releases

| Event | What happens |
|---|---|
| PR opened/updated | CI: format check, all tests for the changed areas, builds, end-to-end tests |
| Merge to `main` | CD: backend Docker image → GitHub Container Registry; Android build → Firebase App Distribution (once secrets exist); deploy to **staging** only from the Pre-launch phase (no hosting during development, D-011) |
| Tag `v*` on `main` | release: production deploy (once hosting exists), signed release build for the Play Store |

Staging uses **real services in test mode** with seeded test users for every user type. See [TESTING.md](TESTING.md).

## Daily commands

```bash
./gradlew spotlessApply             # format Kotlin before committing
./gradlew check                     # all Kotlin checks + tests
./gradlew :backend:run              # API with repo-root .env
cd admin && pnpm dev                # admin panel
cd admin && pnpm verify             # admin checks
```

## Working with AI tools

Claude Code reads [CLAUDE.md](../CLAUDE.md). Useful skills: `/add-task` (turn a feature idea into tasks), `/work-task APP-003`
(do a task end to end), `/add-endpoint`, `/db-migration`, `/feature-security-check`. AI output is reviewed like any teammate's PR.
