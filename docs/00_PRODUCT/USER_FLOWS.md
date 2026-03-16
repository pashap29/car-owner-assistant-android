# User Flows

## Navigation Contract
All core flows are launched within the 5-tab system navigation:
- Fuel
- Expense
- Service
- Statistics
- Settings

Mileage screens are non-tab destinations reachable from Fuel, Service, and Statistics.

## 1. First Launch and Setup
### Preconditions
Fresh install and no local dataset.

### Steps
1. User accepts privacy notice.
2. User creates active vehicle profile.
3. User selects units/currency and reminder defaults.
4. App opens Fuel tab as first post-onboarding screen.
5. Reminder engine stays inactive until first car exists.

### Success Criteria
- Vehicle and preferences are saved locally.
- User can access Mileage Entry from Fuel tab CTA.

## 2. Add or Resolve Mileage Entry
### Entry Points
- Fuel tab: Add Mileage action.
- Service tab: mileage correction prompt when required.
- Statistics tab: Mileage Ledger card action.

### Steps
1. User opens Mileage Entry/Ledger screen.
2. Enters odometer and timestamp.
3. Optional: attaches odometer photo.
4. System applies anomaly rules and assigns status.
5. If critical anomaly exists, entry is marked conflicted and user resolves explicitly.

### Verification Outcome Rules
- Manual mileage without odometer photo is not verified.
- Mileage captured via fuel/service context without dedicated odometer photo is not verified.
- Entry is verified only when all conditions are met (dedicated entry, photo attached, manual value input, no active critical anomaly).

### Failure Paths
- Odometer regression (critical anomaly).
- Service mileage lower than latest known mileage (critical anomaly).
- Same-day difference above 2000 km (conflict).

## 3. Log Fuel (Mileage-Integrated)
### Steps
1. User opens Fuel tab and starts new fuel entry.
2. Inputs volume, total cost, date/time, optional mileage context.
3. System validates mileage relationship when mileage provided.
4. Entry saved and fuel metrics updated.

### Success Criteria
- Entry without mileage is valid.
- Mileage-dependent fuel analytics include only eligible records/chains.

## 4. Log Expense
### Steps
1. User opens Expense tab and creates expense record.
2. Selects category, amount, date/time, optional mileage relation.
3. Entry saved to local timeline.

### Rules
- Mileage linkage is recommended but optional for fuel entries, maintenance, repair, and tire-related costs.
- Mileage linkage is optional for insurance, tax, fines, parking, washing, and other costs.
- Records without mileage remain valid but are excluded from mileage-dependent analytics.

## 5. Log Service and Update Due State
### Steps
1. User opens Service tab and records service event.
2. Defines due rule by distance, date, or both.
3. System checks service mileage against latest known mileage.
4. System recalculates due status using mileage + time.
5. Reminder schedule is updated.

### Critical Rule
- Service mileage lower than latest known mileage is a critical anomaly.

## 6. Review Statistics
### Steps
1. User opens Statistics tab.
2. Selects period filter.
3. App evaluates card-specific thresholds.
4. App renders eligible cards and shows instructional empty states for ineligible cards.

### Threshold Rules
- Total expenses card: show from 1 record.
- Category breakdown: show from 3 records.
- Average check: show from 3 records.
- Trend charts: show only with at least 2 time buckets containing data.
- Cost per km: require at least 2 mileage records, at least 50 km between them, and at least 1 expense in the selected period.
- Fuel analytics: require at least 1 valid closed fuel chain.
- Mileage summary cards: require at least 2 mileage records.

## 7. Backup and Restore
### Entry Point
Settings tab -> Backup and Restore screen.

### Backup Steps
1. User starts local export.
2. App validates dataset and builds backup package.
3. Result summary is shown.

### Restore Steps
1. User selects backup package.
2. App validates format and checksums.
3. App automatically creates safety snapshot of current local state.
4. App applies safe replace restore for data covered by backup.
5. App shows restore report.

### Scope Rule
- Merge restore is out of scope for MVP.

## 8. Reminder Cadence
### Rules
- Do not activate reminders before the first car is created.
- If the first car exists but there are no mileage entries, send first reminder after 7 days.
- Repeat every 30 days until first mileage entry exists.
- After first mileage entry, remind after 30 days of mileage inactivity.
- Reminders can be disabled in settings.
