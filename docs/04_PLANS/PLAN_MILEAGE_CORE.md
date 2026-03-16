# Milestone M2: Mileage Core (First-Class Trust Domain)

## Objective
Implement verified mileage as the core trust domain before any analytics finalization.

## Scope
- Mileage data model and persistence.
- Verification state transitions (`verified`, `unverified`, `conflicted`).
- Anomaly detection thresholds and conflict resolution flow.
- Dual-unit storage consistency (`odometerKm`, `odometerMi`).

## Dependencies
- M1 complete.
- `docs/01_DOMAIN/MILEAGE_DOMAIN.md`
- `docs/01_DOMAIN/DATA_MODEL.md`
- `docs/00_PRODUCT/USER_FLOWS.md`

## Step-by-Step Execution
1. Implement mileage entity schema and repositories.
2. Implement verification/anomaly policy and trusted-reference logic.
3. Implement mileage ledger + entry/edit/conflict flows.
4. Add recomputation hooks for dependent features.

## Risks
- False-positive anomalies causing user friction.
- Time ordering and conversion drift between km/mi values.

## Verification
- Domain unit tests for all anomaly thresholds.
- Integration tests for conflict lifecycle and persistence.
- UI tests for mileage add/edit/conflict resolution.

## Acceptance Criteria
- Mileage trust state is deterministic and auditable.
- Critical anomalies always produce conflicted status.
- Dual-unit storage remains consistent within rounding tolerance.
- Contracts are stable for Fuel/Service/Analytics milestones.

## Exit Deliverables
- Stable mileage domain APIs.
- Test coverage for trust/anomaly logic.
- Integration-ready mileage contracts for downstream milestones.
