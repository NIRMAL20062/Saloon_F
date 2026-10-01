# 📱 Android App: Completed

Newest at the bottom. Each entry keeps the commits so anyone can `git show <hash>` to see exactly what changed.

> Groundwork the app already relies on (done in the backend/platform files):
> [BE-003](BACKEND_TASKS_COMPLETED.md#be-003--gradle-build) Gradle build ·
> [BE-004](BACKEND_TASKS_COMPLETED.md#be-004--shared-api-contract-module) shared API contract (the app reuses these models)

## Phase 0: Walking skeleton

### APP-001 · Android project skeleton
- **Completed:** 2026-10-01 · **Commits:** `a0ad079`
- **Built:** `android/app` in the root Gradle build: app name **Glide**, `com.glide.android`, minSdk 24, compile/target SDK 37; Material 3 theme (baseline/dynamic colours until Figma designs), Hilt (`GlideApplication`, `MainActivity`), type-safe Navigation Compose (`HomeRoute` → `HomeScreen`); debug + release (R8 minify + resource shrinking, 968 KB APK); `GLIDE_BACKEND_ONLY=true` skips the module for Docker
- **Tests:** `AppLaunchTest` (1): launches the real `MainActivity` on Robolectric, so the Hilt graph, theme and navigation are all exercised without an emulator
- **Verified on device:** moto g54 5G, Android 15 (API 35): installs, shows the start screen, cold start 2.0 s
- **Security:** `allowBackup=false`, `fullBackupContent=false`, data-extraction rules exclude everything from cloud backup and device transfer; no secrets in code, `BuildConfig` or resources; no INTERNET permission yet (comes with APP-002)
- **Database:** none
- **Fixes along the way:** Espresso pinned to 3.7.0 (Compose's transitive 3.5.0 calls `InputManager.getInstance()`, removed in recent Android); test JVM `--add-opens java.base/jdk.internal.access` for Robolectric on JDK 25
- **Follow-up (needs design):** launcher icon. Lint warns `MissingApplicationIcon` until the Figma asset exists

