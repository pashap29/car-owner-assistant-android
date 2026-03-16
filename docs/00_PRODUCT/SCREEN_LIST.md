# Screen List

## A. System Navigation (Bottom Tabs)
1. Fuel
2. Expense
3. Service
4. Statistics
5. Settings

This 5-tab set is fixed for MVP.

## B. Tab Screens and Purpose
### Fuel (Tab Root)
- Purpose: fast fuel logging and fuel history review.
- Key data: volume, cost, date/time, optional mileage context, fuel type.
- Primary actions: Add Fuel, Add Mileage.

### Expense (Tab Root)
- Purpose: record non-fuel ownership costs.
- Key data: category, amount, date/time, optional mileage relation.
- Primary actions: Add Expense, Filter by period/category.

### Service (Tab Root)
- Purpose: track completed service and future due state.
- Key data: service type, cost, due-distance, due-date, status, optional mileage.
- Primary actions: Add Service, Edit Due Rule.

### Statistics (Tab Root)
- Purpose: show mileage-aware summaries and trend cards.
- Key data: verified mileage coverage, fuel efficiency, spend, due-state indicators.
- Primary actions: Change period, Open Mileage Ledger.

### Settings (Tab Root)
- Purpose: app preferences and data management.
- Key data: units, currency, reminders, privacy options.
- Primary actions: Backup, Restore, Notification preferences.

## C. Non-Tab Screens
### Onboarding and Consent
Collect consent and initialize first vehicle and preferences.

### Vehicle Profile
Create/edit active vehicle profile.

### Mileage Ledger
List mileage entries with statuses: verified, unverified, conflicted.

### Mileage Entry/Edit
Create or update mileage record with anomaly and verification feedback.

### Mileage Conflict Resolution
Resolve critical anomalies with explicit user choice and reason.

### Backup and Restore
Run backup export and safe replace restore; show restore report.

## D. Statistics Card Threshold Contract
- Total expenses card: show from 1 record.
- Category breakdown: show from 3 records.
- Average check: show from 3 records.
- Trend charts: show only with at least 2 time buckets containing data.
- Cost per km: require at least 2 mileage records, at least 50 km between them, and at least 1 expense in the selected period.
- Fuel analytics: require at least 1 valid closed fuel chain.
- Mileage summary cards: require at least 2 mileage records.
- If threshold is not met, show instructional empty state.

## E. Screen-to-Flow Mapping
- First launch -> Onboarding -> Vehicle Profile -> Fuel tab.
- Verified mileage flow -> Mileage Entry/Ledger/Conflict screens.
- Fuel flow -> Fuel tab (+ Mileage Entry when needed).
- Expense flow -> Expense tab.
- Service flow -> Service tab.
- Statistics flow -> Statistics tab (+ Mileage Ledger drill-down).
- Backup/restore flow -> Settings tab -> Backup and Restore screen.

## F. Cross-Screen Requirements
- All core entry and edit screens must work offline.
- Mileage verification/conflict state must be visible where relevant.
- Error states must provide a direct recovery action.
- Future online/auth/premium/family items remain hidden placeholders only.
