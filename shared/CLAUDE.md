# shared/: API contract

Pure Kotlin/JVM (no Android, no Ktor). Compiled into **both** the backend and the Android app, so a change here breaks both
builds at once. That's the point: the two sides can never disagree about the API.

## Rules

- Only `@Serializable` DTOs, route constants (`ApiRoutes`), error codes (`ErrorCodes`) and `ApiJson`. No business logic, no I/O.
- Package per feature: `com.glide.shared.<feature>` (e.g. `com.glide.shared.health`).
- **Backward compatibility**: released app versions keep calling the backend for months.
  - Adding a field: OK if it's nullable or has a default.
  - Removing/renaming a field, changing its type, or removing an enum value: **breaking**. Needs a new `/v2` route or a
    migration plan agreed in the task. Ask first.
- Money is always an integer in **paise** (`Long`), never `Double`. Timestamps are ISO-8601 UTC strings.
- Every DTO change: update `ContractSerializationTest` (pins the exact JSON) **and** `docs/api/openapi.yaml`.

## Commands

```bash
./gradlew :shared:check
```
