# Data Model

## Domain Boundaries
- Vehicle
- Mileage (first-class trust domain)
- Fuel (specialized with QR-assisted entry)
- Expense
- Service
- Reminder
- Search (MVP constrained scope)
- Backup metadata

## Core Entities
### Vehicle
- `vehicleId`
- identity/profile fields
- `isActive`
- lifecycle timestamps

### AppSettings
- unit preferences (display)
- reminder enabled/disabled
- currency and locale settings

### MileageEntry
- `mileageEntryId`
- `vehicleId`
- `timestamp`
- `odometerKm` (stored)
- `odometerMi` (stored)
- `origin` (`dedicated_mileage`, `fuel_context`, `service_context`)
- `userEnteredValue`
- optional odometer photo attachment
- `status` (`verified`, `unverified`, `conflicted`)
- anomaly flags (`critical_anomaly`, `large_jump_suspected`)
- reference to previous trusted mileage entry

### FuelEntry
- `fuelEntryId`
- `vehicleId`
- `timestamp`
- `liters`
- `totalAmount`
- `fuelType`
- `isFullTank`
- optional `odometerKm` / `odometerMi`
- `entryMethod` (`manual`, `qr_assisted`)
- optional station/note

### FuelQrPayload (Fuel specialization)
- `fuelEntryId`
- raw QR payload
- parser version and parse status
- parsed values (amount, timestamp, merchant/station where available)
- parse warnings/errors

### FuelChain (Derived Domain Object)
- derived from ordered fuel entries of one vehicle
- boundaries: `startFullEntryId`, `endFullEntryId` (nullable for open)
- status: `OPEN`, `CLOSED_VALID`, `CLOSED_INVALID`, `IGNORED`
- aggregates:
  - `startOdometerKm`, `endOdometerKm`, `distanceKm`
  - `startOdometerMi`, `endOdometerMi`, `distanceMi`
  - `includedLiters`, `includedAmount`
  - `consumptionLPer100Km`

### ExpenseEntry
- `expenseEntryId`
- `vehicleId`
- `timestamp`
- category
- amount/currency
- optional `odometerKm` / `odometerMi`
- optional note/attachments

### ServiceEntry
- `serviceEntryId`
- `vehicleId`
- `timestamp`
- type/cost
- due-by-distance and/or due-by-date policy
- optional `odometerKm` / `odometerMi`

### ReminderState
- `vehicleId`
- reminders enabled flag
- first car created timestamp
- first mileage recorded timestamp
- last mileage activity timestamp
- next reminder timestamp

### SearchIndexRecord (MVP)
- `searchRecordId`
- `vehicleId`
- `entityType` (`fuel`, `expense`)
- source record id
- normalized searchable text
- searchable timestamp

### BackupSnapshot
- snapshot id
- created timestamp
- snapshot scope metadata

### BackupArtifact
- artifact id
- slot (`backup-new`, `backup-old`)
- manifest version
- created timestamp
- checksum and file reference

## Relationships
- One active vehicle at a time in MVP.
- Vehicle has many mileage, fuel, expense, service records.
- Fuel/service/expense may reference mileage context.
- Fuel chains are recomputed from a vehicle's ordered fuel entries.
- Search index is built only for fuel and expense entries of the active vehicle.
- Backup artifacts include records for supported entities and settings.

## Domain Invariants
- Exactly one active vehicle in MVP.
- Mileage domain is first-class and authoritative for mileage trust.
- `odometerKm` and `odometerMi` are both stored for mileage, fuel (if present), expense (if present), service (if present).
- Dual-unit invariant: `odometerMi` must represent `odometerKm * 0.621371` within configured rounding tolerance.
- Any critical mileage anomaly sets mileage status to `conflicted`.
- Verified mileage requires dedicated/manual/photo/no-critical-anomaly conditions.
- Records without mileage are valid but excluded from mileage-dependent analytics.
- Only `CLOSED_VALID` fuel chains are analytics-eligible.
- Search scope in MVP is strictly limited to fuel and expense entries of the active vehicle.

## Versioning
- explicit DB schema version
- explicit backup format version
- migration notes per release
