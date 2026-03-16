# Android Architecture

## 1. Architecture Goals (MVP)
- Native Android only (Kotlin + Jetpack Compose).
- Offline-first operation for all core records.
- Explicit domain boundaries with testable business rules.
- Stable foundation for future online/auth/premium/family capabilities without shipping them in MVP.

## 2. Architectural Style
Feature-first modular architecture with strict layer separation:
- `ui` layer: Compose screens, UI state models, navigation bindings.
- `domain` layer: entities, value objects, use cases, invariants.
- `data` layer: repository implementations, Room/DataStore/file access, mappers.

Dependency rule:
- `ui -> domain -> data contracts`
- `data` implements domain-owned repository interfaces.
- Domain layer does not depend on Android framework types.

## 3. Module Structure
### App and Core
- `app`: application entry, DI composition root, navigation host.
- `core-common`: constants, error/result primitives, time/clock abstractions.
- `core-model`: shared domain primitives and value objects.
- `core-navigation`: typed route contracts and navigation helpers.
- `core-database`: Room database, DAOs, migrations.
- `core-datastore`: settings and lightweight key-value persistence.
- `core-files`: file/media and backup artifact handling.
- `core-flags`: feature flag evaluation and cache.

### Feature Modules (MVP)
- `feature-onboarding`
- `feature-vehicle`
- `feature-mileage`
- `feature-fuel`
- `feature-expense`
- `feature-service`
- `feature-statistics`
- `feature-settings`
- `feature-backup`
- `feature-search` (MVP scope: fuel + expense of active car only)

### Future-Ready Placeholder Contracts (No MVP implementation)
- `contract-auth`
- `contract-cloud-sync`
- `contract-premium`
- `contract-family`

These contract modules expose interfaces and models only; runtime implementations are not in MVP.

## 4. Product-Critical Domain Constraints
- Mileage is a first-class trust domain (`verified`, `unverified`, `conflicted`).
- Both `odometerKm` and `odometerMi` are stored where mileage is captured.
- Fuel consumption analytics are based only on `CLOSED_VALID` fuel chains.
- Restore pipeline must use safe-replace and pre-restore safety snapshot.
- Backup rotation uses `backup-new` and `backup-old` slots.
- Search scope is limited to fuel and expense records of active car.

## 5. Runtime Data Flow
1. UI triggers use case.
2. Use case validates domain invariants.
3. Repository performs transactional write/read via Room/DataStore/files.
4. Domain emits updated state via Flow.
5. UI renders state and deterministic empty/error handling.

## 6. Background Work
WorkManager jobs in MVP:
- reminder scheduling and re-scheduling
- backup export/import orchestration
- media cleanup and temporary file pruning
- optional search-index rebuild after bulk restore

## 7. Error and Recovery Strategy
- Use typed domain errors mapped to user-actionable UI messages.
- Never silently ignore mileage conflicts or restore validation failures.
- Keep deterministic retry semantics for file/backup operations.

## 8. Security and Privacy Boundaries
- Local-first by default; no mandatory account/auth in MVP.
- Least-privilege file access in app-scoped storage.
- Sensitive media metadata handling (for example EXIF stripping based on settings).

## 9. Definition of Architecture Completion
Architecture is implementation-ready when:
- module boundaries and dependencies are fixed,
- navigation routes map to approved product flows,
- storage and backup contracts encode domain invariants,
- future online/auth/premium/family features remain interface placeholders only.
