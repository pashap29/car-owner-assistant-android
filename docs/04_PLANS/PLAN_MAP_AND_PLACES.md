# Milestone M6: Map and Places (Optional MVP-Adjacent)

## Objective
Introduce optional map/place enhancements without blocking core MVP flows.

## Scope
- Provider-abstracted place search and map preview.
- Permission-aware/manual fallback behavior.
- Feature-flag-controlled rollout (`map_places_enabled`).

## Dependencies
- M3 complete (required, feature integration point).
- `docs/02_ARCHITECTURE/MAP_PROVIDER_CONTRACT.md`
- `docs/02_ARCHITECTURE/FEATURE_FLAGS.md`

## Step-by-Step Execution
1. Implement provider adapter behind contracts.
2. Implement map/place UI gated by feature flag.
3. Implement permission/network fallback behavior.
4. Validate no-blocking behavior for manual data entry.

## Risks
- Provider SDK leakage into domain/UI contracts.
- Optional feature accidentally blocking core entry flows.

## Verification
- Adapter contract tests.
- Permission denied + no network scenario tests.
- Feature-flag off behavior tests.

## Acceptance Criteria
- Core flows work fully with map feature disabled.
- Map/place integration respects contract and fallback rules.
- Provider abstraction remains replaceable.

## Exit Deliverables
- Optional enhancement feature ready for controlled rollout.
- No impact on critical MVP path when disabled.
