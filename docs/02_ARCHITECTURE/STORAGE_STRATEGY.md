# Storage Strategy

## 1. Storage Allocation
- Room: transactional domain records.
- DataStore: app settings, unit preferences, toggles.
- App-scoped files: media artifacts, backup artifacts, temporary imports.

## 2. Source of Truth
- Room is canonical for vehicle, mileage, fuel, expense, service, reminder states.
- Derived artifacts (fuel chains, analytics views, search index) are recomputable.

## 3. Core Persistence Model
### Room Entities (MVP)
- vehicle
- mileage_entry
- fuel_entry
- fuel_qr_payload
- expense_entry
- service_entry
- reminder_state
- search_index_record
- backup_snapshot_metadata
- backup_artifact_metadata

### Unit Model
- Persist both km and mi where mileage is captured.
- Enforce conversion invariant via domain validation.

## 4. Transaction and Integrity Rules
- Mileage write operations are transactional with anomaly evaluation.
- Fuel-chain-relevant writes trigger deterministic chain recalculation.
- Restore operations are transactional at dataset scope after snapshot creation.
- Search index rebuild should occur after bulk restore completion.

## 5. Backup Storage Strategy
- Maintain two artifact slots:
  - `backup-new` (latest valid)
  - `backup-old` (previous valid)
- Rotate slots only after successful creation of new artifact.
- Keep both slots unchanged if backup generation fails.

## 6. Restore Strategy
- Safe replace only in MVP (no merge mode).
- Steps:
  1. validate manifest/checksums,
  2. create safety snapshot,
  3. replace covered dataset,
  4. rebuild derived indexes/views,
  5. produce restore report.

## 7. Search Storage Scope (MVP)
- Search index stores only fuel and expense records.
- Index is constrained to active-car context.
- Cross-car search is not stored/exposed.

## 8. Migration Policy
- Explicit schema versioning with migration tests.
- No destructive migration for production channels.
- Backup manifest version evolves independently of DB schema version.

## 9. Security and Privacy
- Validate imported payload integrity before applying restore.
- Use app-scoped storage and avoid broad file permissions.
- Respect settings for media metadata stripping and retention.
