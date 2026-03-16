# Milestone M1: Bootstrap Android (Runnable Skeleton)

## Objective
Produce a runnable native Android project skeleton aligned with approved architecture and coding standards.

## Scope
- Initialize Gradle/AGP/Kotlin baseline.
- Create core and feature module scaffolding per `docs/02_ARCHITECTURE/ANDROID_ARCHITECTURE.md`.
- Add Compose app shell and startup navigation host.
- Wire baseline CI checks (build, unit test, lint).

## Dependencies
- `AGENTS.md`
- `docs/02_ARCHITECTURE/ANDROID_ARCHITECTURE.md`
- `docs/03_QUALITY/CODING_CONVENTIONS.md`

## Step-by-Step Execution
1. Initialize project structure and module graph.
2. Add app shell with minimal start destination routing.
3. Add baseline dependency injection composition root.
4. Configure CI commands and local dev commands.

## Risks
- Toolchain incompatibility across AGP/Kotlin/Gradle versions.
- Over-scaffolding modules before first feature delivery.

## Verification
- `./gradlew :app:assembleDebug`
- `./gradlew test`
- `./gradlew lint`
- Manual install/run on emulator.

## Acceptance Criteria
- App launches and shows runnable shell.
- Module boundaries compile and resolve.
- CI build/test/lint pipeline passes.
- No product feature logic implemented yet.

## Exit Deliverables
- Runnable skeleton repository.
- Stable module naming and package layout.
- Baseline development and CI workflow.
