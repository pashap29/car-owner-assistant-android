# Files and Media

## 1. Purpose
Define file/media handling for mileage evidence, receipts, and backup artifacts in offline-first MVP.

## 2. Supported Artifact Types
- Mileage odometer photo evidence.
- Fuel/expense/service receipt images.
- Backup artifacts (`backup-new`, `backup-old`).
- Temporary import/scan files (including QR capture intermediates).

## 3. Directory Layout (App-Scoped)
- `files/media/mileage/`
- `files/media/receipts/`
- `files/backup/`
- `cache/import-temp/`
- `cache/scan-temp/`

## 4. Naming and Metadata
- Deterministic file name pattern: `<entityType>_<entityId>_<timestamp>.<ext>`
- Persist metadata in DB:
  - mime type
  - byte size
  - checksum
  - created/updated timestamps
  - source (`camera`, `gallery`, `qr_scan`, `import`)

## 5. Lifecycle Rules
- On record deletion, apply retention policy and unlink artifacts safely.
- Include only linked media in backup package.
- On restore, deduplicate by checksum when compatible.
- Clear temporary directories after import/scan completion or failure.

## 6. Backup Artifact Handling
- Keep only two rotated backup artifacts (`backup-new`, `backup-old`) plus current safety snapshot when applicable.
- Prevent orphaned backup files by syncing file operations with backup metadata updates.

## 7. Security and Privacy
- Validate file type and size before persistence.
- Respect scoped storage boundaries.
- Strip EXIF location data when user disables location retention.
- Do not expose private app file paths outside app context.

## 8. Performance Rules
- Generate thumbnails for list rendering.
- Lazy-load full-size images on demand.
- Avoid storing duplicate payloads when checksum matches existing file.
