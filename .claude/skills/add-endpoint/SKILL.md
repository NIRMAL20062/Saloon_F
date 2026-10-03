---
name: add-endpoint
description: Add or change a backend API endpoint the Glide way, across shared DTOs, Ktor route/service/repository, OpenAPI spec and tests. Use whenever a task needs a new or changed /v1 endpoint.
---

# Add an API endpoint

Only for an endpoint a task in `tasks/BACKEND_TASKS.md` asks for. Commit after each numbered step that builds green.

## 1. Contract (`shared/`)

- Request/response DTOs in `com.glide.shared.<feature>`, `@Serializable`, money as `Long` paise.
- Path constant in `ApiRoutes` under `V1`, with the D-017 prefix for its side: `"${V1}/salon/services"` (salon side; the salon
  comes from the signed-in person's membership, D-036), `"${V1}/c/bookings"` (customer side), `"${V1}/admin/salons"` (admin).
- Feature-specific error codes next to the feature (`<Feature>ErrorCodes`), stable `UPPER_SNAKE` strings.
- Extend `ContractSerializationTest` (or a feature test) to pin the JSON.
- Update `docs/api/openapi.yaml`: path, request, every response status, error envelope.

## 2. Database (if needed)

Use `/db-migration`. Exposed table object in `backend/.../<feature>/<Feature>Table.kt`, matching the migration exactly.

## 3. Backend (`backend/.../<feature>/`)

- `<Feature>Repository`: Exposed queries only. Every query on a salon-owned table has `salon_id = :salonId`, where the salon
  id comes from the signed-in person's membership, with a role check (D-036), never from the body. A customer's own data is
  filtered by their user id (D-025).
- `<Feature>Service`: business rules, validation (lengths, ranges, formats), transactions for multi-row changes.
- `<Feature>Routes.kt`: `fun Route.<feature>Routes(service)`. Parse → validate → service → respond with DTO. Register it in `Application.module`.
- Validation failures → 400 `BAD_REQUEST` (or a feature code) with a message that names the field but never echoes secrets.
- Missing resource, resource of another salon, **or another customer's** → 404 (don't reveal it exists).

## 4. Tests (`backend/src/test/.../<feature>/`)

Real Postgres via `TestDatabase`. Minimum per endpoint:
- [ ] happy path (status + exact body)
- [ ] each validation rule rejected with 400
- [ ] not found → 404
- [ ] **other salon's data → 404** (tenant isolation), once tenants exist
- [ ] **other customer's data → 404** (customer-side endpoints)
- [ ] unauthenticated → 401, wrong role → 403, once auth exists
- [ ] database state after the call is correct (query it, don't trust the response)
- [ ] idempotency, if the endpoint can be retried (payments, webhooks)

## 5. Finish

`./gradlew spotlessApply check`. Then the client tasks (`APP-`/`WEB-`) can use it; admin regenerates its TypeScript types from the spec.
