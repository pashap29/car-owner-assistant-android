# Android PRD

## Product Vision
Build a native Android car owner assistant that maintains a trusted operational history with verified mileage as the core trust anchor.

## Product Positioning
- Platform: native Android only.
- Interaction model: offline-first local app.
- Trust model: mileage validation drives reliability of fuel, service, and statistics.

## Core Product Decisions (Normalized)
1. System bottom navigation has exactly 5 tabs: Fuel, Expense, Service, Statistics, Settings.
2. Verified mileage is a first-class product capability, implemented as dedicated non-tab flows.
3. Fuel and service records integrate with mileage context.
4. Statistics apply mileage-aware quality gates and never show misleading zero-value analytics.
5. Online/auth/premium/family features are architecture placeholders only for MVP.

## Resolved Ambiguity Decisions (Implementation-Blocking, Now Resolved)
### 1. Verified Mileage Policy
- Manual mileage without odometer photo is not verified.
- Mileage from fuel or service records without dedicated odometer photo is supported, but not verified.
- A mileage entry is marked as verified only if:
  a) it is a dedicated mileage entry or odometer-photo entry,
  b) an odometer photo is attached,
  c) the user manually entered the mileage value,
  d) there is no active critical anomaly against the previous trusted mileage record.
- Any mileage entry with a critical anomaly is conflicted.

### 2. Mileage Anomaly Thresholds
- Critical anomaly if new mileage is lower than previous mileage.
- Critical anomaly if a service mileage is lower than the latest known mileage.
- Same-day entries may differ by up to 50 km without anomaly.
- Same-day difference above 2000 km is a conflict.
- Flag large_jump_suspected if:
  a) mileage delta > 5000 km in 3 days, or
  b) mileage delta > 100000 km in 30 days, or
  c) average delta > 1000 km/day over at least 3 days.
- For nearby entries within 7 days, do not raise a hard conflict if the difference is within 5% of the new value and not more than 500 km.

### 3. Expense Categories and Mileage Linkage
- Mileage linkage is recommended but optional for fuel entries, maintenance, repair, and tire-related costs.
- Mileage linkage is optional for insurance, tax, fines, parking, washing, and other costs.
- Records without mileage are still valid, but must be excluded from mileage-dependent analytics.

### 4. Minimum Thresholds for Statistics Cards
- Total expenses card: show from 1 record.
- Category breakdown: show from 3 records.
- Average check: show from 3 records.
- Trend charts: show only with at least 2 time buckets containing data.
- Cost per km: require at least 2 mileage records, at least 50 km between them, and at least 1 expense in the selected period.
- Fuel analytics: require at least 1 valid closed fuel chain.
- Mileage summary cards: require at least 2 mileage records.
- If a card threshold is not met, show an instructional empty state instead of zero-value analytics.

### 5. Backup Restore Policy
- Default restore policy is safe replace, not merge.
- Before restore, automatically create a safety snapshot of the current local state.
- Restore fully replaces the current local dataset covered by the backup.
- Merge restore is out of scope for MVP.

### 6. First-Time Reminder Cadence
- Do not activate reminders before the first car is created.
- If the first car exists but there are no mileage entries, send the first reminder after 7 days.
- Then repeat every 30 days until the first mileage entry exists.
- After the first mileage entry, remind after 30 days of mileage inactivity.
- Reminders can be disabled in settings.

## MVP Functional Requirements
- FR-01: First launch onboarding captures consent, vehicle profile, units, and reminder defaults.
- FR-02: App supports one active vehicle profile in MVP.
- FR-03: Mileage Ledger supports add/edit/review/conflict-resolution with verification status.
- FR-04: Mileage validation and anomaly policy follow the resolved thresholds in this document.
- FR-05: Fuel logging supports volume/cost/date and optional mileage linkage.
- FR-06: Expense logging supports categorized costs and optional mileage linkage.
- FR-07: Service logging supports due-by-distance and due-by-date rules.
- FR-08: Statistics tab enforces card thresholds and instructional empty states.
- FR-09: Local backup and safe-replace restore are available from Settings.
- FR-10: Core user flows (fuel, expense, service, mileage, backup) work without internet.
- FR-11: Reminder cadence follows the resolved policy and is user-disableable.

## MVP Non-Functional Requirements
- NFR-01: Local persistence is the source of truth for all core records.
- NFR-02: No core action may require account login.
- NFR-03: Errors must provide corrective next action.
- NFR-04: Verification/conflict state is visible in mileage-dependent flows.

## Success Metrics (MVP)
- Weekly logging activity rate.
- Verified mileage ratio over total mileage entries.
- Fuel entries linked to valid mileage context.
- Service reminder completion adherence.

## Explicit Non-Goals (MVP)
- Cross-platform parity.
- Real-time cloud sync.
- Fleet/multi-driver operations.
- Premium subscription features.

## Ambiguity Status
All previously listed implementation-blocking ambiguities for verification, thresholds, analytics cards, backup restore policy, and reminder cadence are resolved.
