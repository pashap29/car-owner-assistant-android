# Mileage Domain

## Objective
Model mileage as a first-class trust domain that governs statistics quality and cross-domain consistency.

## First-Class Domain Role
- Mileage is not a secondary field.
- Fuel, service, expense-per-distance, and distance-based reminders depend on mileage trust status.

## Stored Unit Model (Resolved)
- Mileage values are stored in both units:
  - `odometerKm`
  - `odometerMi`
- Input may be provided in either unit, but both stored values must be persisted.
- Canonical validation is performed on km values; miles are synchronized using conversion invariant.

## Status Model
- `verified`
- `unverified`
- `conflicted`

## Verified Mileage Policy (Resolved)
- Manual mileage without odometer photo is not verified.
- Mileage from fuel or service records without dedicated odometer photo is supported, but not verified.
- A mileage entry is marked as verified only if:
  a) it is a dedicated mileage entry or odometer-photo entry,
  b) an odometer photo is attached,
  c) the user manually entered the mileage value,
  d) there is no active critical anomaly against the previous trusted mileage record.
- Any mileage entry with a critical anomaly is conflicted.

## Trusted Mileage Reference
- Previous trusted mileage record = latest prior mileage record not in `conflicted` status and accepted by resolution policy.
- Critical anomaly checks compare against previous trusted mileage reference.

## Critical Anomaly Rules (Resolved)
- Critical anomaly if new mileage is lower than previous mileage.
- Critical anomaly if a service mileage is lower than the latest known mileage.
- Same-day difference above 2000 km is a conflict.

## Non-Critical Threshold Rules (Resolved)
- Same-day entries may differ by up to 50 km without anomaly.
- Flag `large_jump_suspected` if:
  a) mileage delta > 5000 km in 3 days, or
  b) mileage delta > 100000 km in 30 days, or
  c) average delta > 1000 km/day over at least 3 days.
- For nearby entries within 7 days, do not raise a hard conflict if the difference is within 5% of the new value and not more than 500 km.

## Conflict Resolution
- Never auto-overwrite conflicted records.
- Keep explicit resolution path with reason.
- Recompute dependent statistics/reminder calculations after resolution.

## Cross-Domain Effects
- Fuel chains with included critical-anomaly mileage become `CLOSED_INVALID`.
- Service due-state recomputation must use trusted mileage context.
- Expense/fuel records without mileage remain valid but excluded from mileage-dependent analytics.

## Derived Outputs
- Distance since last entry (km + mi display forms).
- Distance since last service baseline.
- Data quality indicators for gaps and suspected jumps.

## Test Priorities
- Monotonic and regression edge cases.
- Same-day threshold boundaries (50 km and 2000 km).
- Nearby-window tolerance boundaries (7 days, 5%, 500 km).
- Large jump suspicion flags across 3-day and 30-day windows.
- Dual-unit conversion consistency and rounding boundaries.
