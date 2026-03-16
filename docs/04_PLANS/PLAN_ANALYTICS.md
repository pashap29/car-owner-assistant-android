# Milestone M7: Analytics and Statistics

## Objective
Deliver statistics features using trusted mileage and eligible fuel/expense/service inputs only.

## Scope
- Statistics card calculations and thresholds.
- Data-quality gating and instructional empty states.
- Metric pipelines for mileage, fuel, expense, service insights.

## Dependencies
- M2 complete (required, mileage trust foundation).
- M3 complete (required, fuel/expense inputs).
- M4 complete (required, service reminder/adherence inputs).
- M5 complete (required, stable persistence/restore behavior).
- `docs/01_DOMAIN/ANALYTICS_RULES.md`

## Step-by-Step Execution
1. Implement metric pipelines from approved input contracts.
2. Implement threshold gating and empty-state behavior.
3. Implement aggregation filters for trust/eligibility constraints.
4. Validate analytics consistency across restore/recompute scenarios.

## Risks
- Misleading metrics from invalid/incomplete inputs.
- Formula drift from approved domain definitions.
- Performance regressions with larger local datasets.

## Verification
- Formula unit tests and fixture-based comparisons.
- Integration tests across mileage/fuel/expense/service datasets.
- UI tests for threshold and empty-state behavior.

## Acceptance Criteria
- Verified mileage constraints are enforced before metric aggregation.
- Fuel metrics use only `CLOSED_VALID` chains.
- Card thresholds match approved product rules exactly.
- Statistics remain stable after backup/restore and recomputation.

## Exit Deliverables
- MVP statistics implementation ready for release hardening.
- Baseline KPI layer for post-release monitoring.
