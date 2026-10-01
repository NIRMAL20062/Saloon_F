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

### APP-002 · Network layer on the shared contract
- **Completed:** 2026-10-01 · **Commits:** `e67dbdd` (DF-10 change), `41de918`
- **Built:** `data/network/`: `GlideApi` (Retrofit, paths + models from `:shared`), `createOkHttpClient` / `createRetrofit` (plain functions, so tests use the exact app setup), `RequestIdInterceptor` (`X-Request-Id: android-<uuid>`), `apiCall {}` → `ApiResult` / `ApiError` (`Network`, `Http(status, code, requestId)`, `Unexpected`), Hilt `NetworkModule`; `BuildConfig.API_BASE_URL` per build type
- **Tests:** `NetworkLayerTest` (8, MockWebServer): success, unknown fields ignored, error envelope, non-envelope error, no connection, contract violation, request ID format accepted by backend, `/health` 503 body readable. `NetworkSecurityConfigTest` (3): release has no cleartext, debug cleartext only to localhost, base URL per build type
- **Security:** release HTTPS-only with system CAs; debug cleartext only to `127.0.0.1`/`localhost` (reached via `adb reverse`); body logging debug-only with `Authorization` redacted; raw server text never leaves the network layer (only error **codes**); release URL is a non-resolving `.invalid` placeholder until Q-004
- **Database:** none
- **Changed from plan:** debug URL is `http://127.0.0.1:8080` + `adb reverse` instead of `10.0.2.2`, so real phones work too (DF-10 updated)

