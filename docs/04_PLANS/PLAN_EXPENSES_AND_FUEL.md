# Milestone M3: Expenses and Fuel (Mileage-Integrated)

## Objective
Deliver expense and fuel flows with strict mileage integration and valid fuel-chain behavior.

## Scope
- Expense and fuel record creation/editing.
- Fuel chain derivation and status evaluation.
- QR-assisted fuel entry path (manual confirmation required).
- Mileage-dependent metric inputs and exclusions.

## Dependencies
- M2 complete (required).
- `docs/01_DOMAIN/FUEL_DOMAIN.md`
- `docs/01_DOMAIN/ANALYTICS_RULES.md`
- `docs/01_DOMAIN/DATA_MODEL.md`
- `docs/03_QUALITY/TEST_STRATEGY.md`

## Step-by-Step Execution
1. Implement expense and fuel data contracts.
2. Implement fuel-chain builder, statuses, invalidation rules.
3. Implement QR-assisted fuel parsing and confirm-before-save flow.
4. Enforce analytics eligibility (`CLOSED_VALID` chains only).
5. Enforce mileage-optional records with exclusion from mileage-dependent metrics.

## Risks
- Incorrect chain derivation under edge ordering cases.
- Consumption distortion if ineligible chains are included.
- QR parsing ambiguity leaking invalid values.

## Verification
- Required fixture set from `TEST_STRATEGY.md`.
- Unit tests for chain closure/invalidation/inclusion rules.
- Integration tests for mileage-fuel recomputation after mileage edits.
- Offline flow tests for expense/fuel entry.

## Acceptance Criteria
- Fuel and mileage integration matches domain rules exactly.
- All required fuel-chain fixtures pass.
- Expense/fuel records without mileage remain valid and correctly excluded from mileage-dependent analytics.
- QR-assisted path never bypasses validation.

## Exit Deliverables
- Reliable fuel and expense domain flows.
- Deterministic fuel-chain analytics inputs.
- Stable contracts for service and analytics milestones.
