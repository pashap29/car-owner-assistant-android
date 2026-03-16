# Feature Flags

## 1. Objective
Control rollout risk while preserving stable core mileage/fuel/expense/service behavior in MVP.

## 2. Flag Architecture
### Components
- `FeatureFlagRepository` (domain-facing API)
- `LocalFlagSource` (DataStore-backed defaults/cache)
- `RemoteFlagSource` (future placeholder interface only)

### Resolution Order
1. local hardcoded safe default
2. local persisted override
3. remote value (future, optional)

If remote is unavailable, system uses local value without blocking flows.

## 3. Flag Classes
- `release`: controlled rollout of optional features
- `experiment`: short-lived product experiments
- `ops`: emergency kill switches

## 4. MVP Rules
- Core mileage trust flow must not be controlled by remote flags.
- Flags must fail-safe to behavior that preserves data integrity.
- Flags must be evaluable offline.
- Flag evaluation must be deterministic and observable in debug logs.

## 5. Initial Flag Registry
- `map_places_enabled`
  - class: `release`
  - default: `false`
  - scope: optional map/place UI only
- `advanced_analytics_enabled`
  - class: `release`
  - default: `false`
  - scope: non-MVP statistics cards only
- `encrypted_backup_enabled`
  - class: `release`
  - default: `false`
  - scope: optional backup encryption path
- `future_auth_contract_enabled`
  - class: `release`
  - default: `false`
  - scope: placeholder wiring only, no MVP auth UI
- `future_cloud_sync_contract_enabled`
  - class: `release`
  - default: `false`
  - scope: placeholder wiring only, no MVP sync
- `future_premium_contract_enabled`
  - class: `release`
  - default: `false`
  - scope: placeholder wiring only, no MVP premium
- `future_family_contract_enabled`
  - class: `release`
  - default: `false`
  - scope: placeholder wiring only, no MVP family features

## 6. Governance
- Every flag must have owner, rationale, add date, target removal date.
- Remove stale flags after rollout finalization.
- Do not introduce cross-cutting flags without architecture review.

## 7. Testing Expectations
- Unit tests for resolution order and fallback behavior.
- Offline behavior tests for core flows when remote is unavailable.
- Regression tests for kill-switch behavior on optional modules.
