# Analytics Rules

## Purpose
Define consistent metric rules and event semantics for user insights.

## Principles
- Metrics must be traceable to source records.
- Deterministic formulas over opaque heuristics.
- Explicit quality/confidence states for estimates.
- Instructional empty states instead of misleading zero-value cards when thresholds are not met.

## Event Contract
- Stable naming convention for domain events.
- Versioned schema when breaking changes occur.
- No sensitive personal payload by default.

## Analytics Input Contracts (MVP)
### Mileage Inputs
- source: `MileageEntry`
- required for mileage-dependent metrics
- must use trusted mileage status rules (`verified`/resolved `unverified` where allowed by metric)

### Fuel Inputs
- source: `FuelEntry` + derived `FuelChain`
- final fuel-consumption analytics use only `CLOSED_VALID` chains
- chain closure, inclusion, invalidation rules are normative in `FUEL_DOMAIN.md`

### Expense Inputs
- source: `ExpenseEntry`
- records without mileage are valid for totals and category spend
- records without mileage are excluded from mileage-dependent metrics

### Service Inputs
- source: `ServiceEntry`
- used for due-state and adherence metrics

### Scope Filter
- MVP analytics are calculated for active car context.

## Eligibility Rules
- Records without mileage are valid records.
- Records without mileage are excluded from mileage-dependent analytics.
- Unresolved conflicted mileage is excluded from trusted aggregates.

## Fuel-Chain Analytics Contract (Resolved)
- Fuel chains are derived from ordered fuel entries of one car.
- Chain derivation, closure, included liters/amount, invalidation, and status model are normative in `docs/01_DOMAIN/FUEL_DOMAIN.md`.
- A chain is analytics-eligible only when status is `CLOSED_VALID`.
- `OPEN`, `CLOSED_INVALID`, and `IGNORED` chains are excluded from final fuel-consumption analytics.
- Fuel consumption KPI is computed only from eligible chains.

## Fuel Analytics Dependencies (Implementation Contract)
- Dependency 1: mileage domain must provide critical anomaly status per linked entry.
- Dependency 2: fuel domain must provide deterministic chain status and aggregates.
- Dependency 3: statistics layer must filter to `CLOSED_VALID` only before aggregation.
- Dependency 4: if no eligible chains exist, show instructional empty state.

## Unit Handling Rules
- Analytics calculations use km-native formulas where specified.
- Both km and mi values remain available for display conversion.
- Display conversion must not change underlying computed aggregates.

## Statistics Card Thresholds (Resolved)
- Total expenses card: show from 1 record.
- Category breakdown: show from 3 records.
- Average check: show from 3 records.
- Trend charts: show only with at least 2 time buckets containing data.
- Cost per km: require at least 2 mileage records, at least 50 km between them, and at least 1 expense in the selected period.
- Fuel analytics: require at least 1 valid closed fuel chain (`CLOSED_VALID`).
- Mileage summary cards: require at least 2 mileage records.
- If a card threshold is not met, show an instructional empty state instead of zero-value analytics.

## Metric Families
- Activity and logging consistency.
- Verified mileage ratio.
- Fuel efficiency and cost trends.
- Service adherence.

## Validation
- Unit tests for formulas.
- Dataset-based regression checks for chart-ready outputs.
- Explicit threshold boundary tests for every statistics card.
- Chain-status eligibility tests for fuel analytics inclusion/exclusion.
- Unit-conversion consistency tests for km/mi display paths.
