# Navigation

## 1. Navigation Principles
- Follow fixed bottom navigation with 5 tabs: `Fuel`, `Expense`, `Service`, `Statistics`, `Settings`.
- Mileage flows are non-tab destinations reachable from Fuel/Service/Statistics contexts.
- Preserve tab state and user context where possible.

## 2. Graph Topology
### Root Graph
- `OnboardingGraph`
- `MainGraph`

### OnboardingGraph
- Consent
- Vehicle Setup
- Preferences Setup

Exit condition: active vehicle created.

### MainGraph (Bottom Tabs)
- `FuelTabGraph`
- `ExpenseTabGraph`
- `ServiceTabGraph`
- `StatisticsTabGraph`
- `SettingsTabGraph`

### Non-Tab Flow Graphs
- `MileageGraph`
  - Mileage Ledger
  - Mileage Entry/Edit
  - Mileage Conflict Resolution
- `BackupGraph`
  - Backup/Restore screen
  - Restore report screen
- `FuelQrGraph` (from Fuel tab)
  - QR scan/import
  - Parse review/confirm
- `SearchGraph`
  - Search (fuel + expense of active car)
  - Search result details

## 3. Start Destination Rules
- No active vehicle -> start `OnboardingGraph`.
- Active vehicle exists -> start `MainGraph` with default tab `Fuel`.

## 4. Tab Behavior Rules
- Tabs keep independent back stacks in MVP.
- Switching tabs restores last state in each tab.
- Back from tab root follows system back behavior (exit or previous task state).

## 5. Route Contracts
- Use typed route arguments only (IDs, filters, period tokens).
- Reject invalid IDs with safe fallback to tab root + non-blocking error message.
- No free-form Bundle contracts.

## 6. Flow Mappings (Product-Aligned)
- Fuel logging -> `FuelTabGraph` (+ `MileageGraph` when needed).
- Expense logging -> `ExpenseTabGraph`.
- Service logging/reminders -> `ServiceTabGraph` (+ `MileageGraph` if conflict resolution needed).
- Statistics review -> `StatisticsTabGraph` (+ `MileageGraph` drill-down).
- Backup/restore -> `SettingsTabGraph` -> `BackupGraph`.

## 7. State Restoration
- Restore selected tab after process recreation.
- Restore in-progress form drafts for fuel/expense/service/mileage entry screens.
- Restore period/filter state for statistics and search.

## 8. Deep Link Policy (MVP)
- Internal deep links are supported for stable route contracts.
- External/public deep links are not required for MVP.
- Any future external deep links must preserve active-car scope checks.

## 9. Guardrails
- Navigation must never bypass mileage conflict resolution when required.
- Restore flow must not expose merge mode in MVP.
- Search destination must enforce active-car + entity-type (`fuel`,`expense`) constraint.
