# Milestone: Garage

## Objective
Implement local garage management for multiple cars with exactly one active car and a persisted app start mode.

## Scope
- Create, edit, delete vehicles.
- Keep one active vehicle at a time.
- Persist garage records locally in Room.
- Persist garage startup preference in DataStore.
- Expose garage flow from app startup and Settings.
- Add unit tests for core garage rules.

## Dependencies
- `docs/01_DOMAIN/DATA_MODEL.md`
- `docs/02_ARCHITECTURE/ANDROID_ARCHITECTURE.md`
- `docs/02_ARCHITECTURE/NAVIGATION.md`
- `docs/02_ARCHITECTURE/STORAGE_STRATEGY.md`

## Execution Order
1. Expand garage domain contracts and startup preference model.
2. Implement Room vehicle persistence and active-car switching rules.
3. Implement DataStore startup mode persistence.
4. Implement garage UI flow and Settings entry point.
5. Add unit tests for garage rules and launch resolution.

## Risks
- Existing product docs describe a single active vehicle profile; garage introduces multi-car storage while preserving only one active car.
- Deleting the active car must deterministically choose the next active car.
- Startup destination must remain stable across process recreation.

## Verification
- `./gradlew :app:assembleDebug`
- `./gradlew :feature-vehicle:testDebugUnitTest`
- Manual restart verification after creating/editing/selecting active car and changing startup mode.

## Acceptance Criteria
- User can create, edit, delete vehicles locally.
- User can select exactly one active vehicle.
- Startup preference survives restart.
- App opens Garage on first launch without vehicles.
- App opens Garage or main shell based on stored startup mode.
