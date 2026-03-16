# Backup Rules

## Scope
Define what is backed up, how integrity is checked, and how restore is applied in MVP.

## Included Data
- Vehicle profile
- Mileage/fuel/expense/service records
- Verification metadata
- Relevant settings
- Reminder state

## Excluded Data
- Temporary cache
- Recomputable derived aggregates
- Rebuildable search index artifacts

## Format
- Versioned manifest
- Structured payload files
- Checksums per payload

## Backup Rotation Policy (Resolved)
- Maintain two backup slots only:
  - `backup-new`: latest successful backup
  - `backup-old`: previous successful backup
- On successful backup creation:
  1. Move current `backup-new` to `backup-old` (overwrite existing `backup-old`).
  2. Write newly generated backup as `backup-new`.
- If new backup generation fails, keep existing `backup-new` and `backup-old` unchanged.
- Rotation applies per active local dataset in MVP.

## Restore Policy (Resolved)
- Default restore policy is safe replace, not merge.
- Before restore, automatically create a safety snapshot of the current local state.
- Restore fully replaces the current local dataset covered by the backup.
- Merge restore is out of scope for MVP.

## Integrity and Security
- Validate checksums before import.
- Abort on corruption with actionable diagnostics.
- Optional encryption path for backup package.

## Test Expectations
- Round-trip backup/restore.
- Pre-restore snapshot creation verification.
- Full-replace dataset verification after restore.
- Rotation behavior verification (`backup-new` / `backup-old`).
- Corruption failure-path coverage.
