# MVP Scope

## Scope Intent
Define what ships now versus later while preserving trust in mileage-dependent functionality.

## NOW (MVP Release Scope)
### Navigation and Shell
- System bottom navigation with 5 tabs only: Fuel, Expense, Service, Statistics, Settings.
- No separate Home tab.

### Core Capabilities
- Single active vehicle profile.
- Dedicated mileage ledger and mileage entry/edit/conflict screens.
- Verified mileage follows the resolved policy in `ANDROID_PRD.md`.
- Fuel logging with optional mileage linkage.
- Expense logging with optional mileage linkage.
- Service logging with due-date and due-distance reminders.
- Statistics tab with explicit card thresholds and instructional empty states.
- Backup/restore in Settings using safe replace only.
- Reminder cadence defined for no-mileage and post-first-mileage states.

### Data and Reliability
- Offline-first CRUD for all core records.
- Deterministic validation for mileage monotonicity/conflicts.
- Conflict states and recovery paths are explicit.
- Records without mileage are valid but excluded from mileage-dependent analytics.

## LATER (Post-MVP, Planned)
- Multi-vehicle support.
- Deeper statistics and forecasting.
- Map/place enrichment for entries.
- Receipt OCR assistance.

## NOT NOW (Explicitly Excluded)
- Cross-platform implementation requirements.
- Mandatory account/authentication.
- Paid premium gating.
- Family sharing and collaborative editing.
- Real-time cloud sync.
- Merge restore mode.

## Architecture Placeholders Only (Future)
- Online sync backend integration points.
- Auth/session contracts.
- Premium entitlement hooks.
- Family/shared-access data contracts.

## Dependency Rules
- Verified mileage must be implemented and stable before statistics are finalized.
- Fuel calculations must consume mileage-validated context.

## MVP Acceptance Criteria
- Bottom navigation exactly matches the 5-tab contract.
- Verification and anomaly policy matches resolved PRD rules.
- Fuel/service flows honor mileage constraints.
- Statistics thresholds and empty-state rules are enforced.
- Restore flow defaults to safe replace with automatic pre-restore snapshot.
- Reminder cadence behavior matches the resolved policy.

## Ambiguity Status
All previously listed MVP ambiguities are resolved and normalized in this document and `ANDROID_PRD.md`.
