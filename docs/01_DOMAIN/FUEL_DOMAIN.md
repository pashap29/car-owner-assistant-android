# Fuel Domain

## Objective
Track fuel data accurately for reliable consumption and cost insights, using chain-based analytics and QR-assisted entry support.

## Specialized Scope (MVP)
- Fuel is a specialized domain because:
  - it supports full/partial chain semantics,
  - it depends on mileage trust,
  - it supports QR-assisted entry.

## Fuel Entry Fields
- `carId`
- `timestamp`
- `liters`
- `totalAmount`
- `fuelType`
- `isFullTank`
- optional `odometerKm`
- optional `odometerMi`
- `entryMethod` (`manual`, `qr_assisted`)
- optional station/note
- optional QR parsing metadata

## QR-Assisted Entry Rules (Resolved)
- QR-assisted entry is supported in MVP for fuel entries only.
- QR payload may prefill amount/date/station and other available fields.
- User must confirm/edit parsed values before save.
- QR parsing failure must not block manual fuel entry.
- `entryMethod = qr_assisted` does not bypass fuel or mileage validation rules.

## Mileage Linkage Rules (Resolved)
- Mileage linkage is recommended but optional for fuel entries.
- Fuel records without mileage are valid.
- Fuel records without mileage are excluded from mileage-dependent analytics.

## Verification Interaction
- Mileage captured through fuel context without dedicated odometer photo is supported but not verified.
- Fuel workflows must not assign verified mileage status on their own.

## Formal Fuel-Chain Definition (Resolved, Normative)
1. Fuel chains are derived domain objects built from ordered fuel entries of a single car.
2. A valid closed fuel chain starts with a fuel entry marked `isFullTank = true` and ends with the next fuel entry marked `isFullTank = true`.
3. Entries between those two full-tank boundaries may be partial or full, but the closing full-tank entry closes the current chain and may also serve as the starting entry of the next chain.
4. Entries before the first full-tank entry do not form a valid chain and are ignored for fuel-consumption analytics.
5. A valid closed fuel chain requires:
   - same car for all entries,
   - ordered timestamps,
   - `liters > 0` for all included entries,
   - `totalAmount > 0` for all included entries,
   - odometer present for all included entries,
   - no odometer decrease inside the chain,
   - no critical mileage anomaly on any included entry,
   - distance between start full and end full `>= 50 km`,
   - total included liters `> 0`.
6. Distance is calculated as:
   - `distanceKm = endOdometerKm - startOdometerKm`
7. Included liters and amount are calculated using:
   - all entries after the starting full-tank entry,
   - plus the closing full-tank entry,
   - excluding the starting full-tank entry itself.
8. Consumption is calculated as:
   - `consumptionLPer100Km = includedLiters / distanceKm * 100`
9. Fuel type differences do not break a chain for the same car in MVP. Fuel type remains available for analytics but is not a chain boundary.
10. Chain statuses must include:
   - `OPEN`
   - `CLOSED_VALID`
   - `CLOSED_INVALID`
   - `IGNORED`
11. If the list ends without a closing full-tank entry, the chain is `OPEN` and must not participate in final fuel-consumption analytics.
12. If a closed chain violates validity rules, it is `CLOSED_INVALID` and must not participate in final fuel-consumption analytics.

## Deterministic Chain Construction Rules
- Derive chains per `carId`; never combine entries from different cars.
- Sort entries by `timestamp` ascending; if timestamps are equal, apply stable tie-break by record ID ascending.
- Mark all entries before the first full-tank entry as `IGNORED` for fuel-consumption analytics.
- When a full entry closes a chain, the same entry is also the starting boundary candidate for the next chain.
- Odometer monotonic check inside a chain uses boundary entries and all entries between boundaries.

## Invalidation Rules (Explicit)
A chain is `CLOSED_INVALID` if any of the following is true:
- at least one included entry has missing odometer,
- odometer decreases at any step within chain boundaries,
- at least one included entry has critical mileage anomaly,
- `distanceKm < 50`,
- included liters sum is `<= 0`,
- any included entry has `liters <= 0` or `totalAmount <= 0`.

## Chain Closure and Analytics Eligibility
- Only `CLOSED_VALID` chains are eligible for final fuel-consumption analytics.
- `OPEN`, `CLOSED_INVALID`, and `IGNORED` chains are excluded from final fuel-consumption analytics.

## Edge Cases (Normative)
- Partial entries only (no full entry): all are `IGNORED` for fuel-consumption analytics.
- Single full entry at end of list: forms `OPEN` chain and is analytics-ineligible.
- Sequence `full -> partial -> full -> partial -> full` yields two closed chains.
- A chain with mixed fuel types can still be `CLOSED_VALID`.
- Records with missing mileage are valid as records, but cannot participate in chain validity checks requiring odometer and therefore make that closed chain invalid for final consumption analytics.

## Search Scope (MVP)
- Fuel search is supported only for entries of the active car.
- Cross-car fuel search is out of scope.

## Integration Contracts
- Depend on mileage ledger as trusted sequence source.
- Recalculate chain status and derived metrics when linked mileage changes.
