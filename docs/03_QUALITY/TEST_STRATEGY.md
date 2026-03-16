# Test Strategy

## Goals
- Protect mileage correctness.
- Prevent offline-flow regressions.
- Keep feedback fast for daily development.

## Pyramid
- Unit: domain rules and calculators.
- Integration: repository, DB, migrations.
- UI: critical user flows.

## Priority Coverage
- Mileage monotonicity/conflict logic.
- Fuel-mileage linkage and fuel-chain derivation.
- Service due logic.
- Backup/restore integrity.

## Fuel-Chain Fixture Scenarios (Required)
- full to full valid
- full partial full valid
- full partial partial full valid
- partial before first full ignored
- full to full with distance below threshold invalid
- chain with decreasing odometer invalid
- chain with critical mileage anomaly invalid
- full only open
- full partial full partial full producing two valid chains

## Fuel-Chain Fixture Expected Outcomes
- `full to full valid` -> `CLOSED_VALID`, eligible.
- `full partial full valid` -> `CLOSED_VALID`, eligible.
- `full partial partial full valid` -> `CLOSED_VALID`, eligible.
- `partial before first full ignored` -> pre-first-full entries marked `IGNORED`.
- `full to full with distance below threshold invalid` -> `CLOSED_INVALID`, ineligible.
- `chain with decreasing odometer invalid` -> `CLOSED_INVALID`, ineligible.
- `chain with critical mileage anomaly invalid` -> `CLOSED_INVALID`, ineligible.
- `full only open` -> `OPEN`, ineligible.
- `full partial full partial full producing two valid chains` -> two independent `CLOSED_VALID` chains, both eligible.

## Fuel-Chain Assertion Rules
- Included liters/amount exclude starting full and include closing full.
- Distance uses end full odometer minus start full odometer.
- Consumption formula is `includedLiters / distanceKm * 100`.
- Non-eligible statuses (`OPEN`, `CLOSED_INVALID`, `IGNORED`) do not contribute to final fuel-consumption analytics.

## Failure/Offline Testing
- No-network operation checks.
- Process recreation/state recovery.
- User-facing error handling paths.

## CI Expectations
- Build + unit tests + lint on every PR.
- Integration/UI smoke checks for release candidates.
