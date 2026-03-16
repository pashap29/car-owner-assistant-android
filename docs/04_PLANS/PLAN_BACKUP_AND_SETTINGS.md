# Milestone M5: Backup and Settings

## Objective
Implement safe data portability and stable preferences before release hardening.

## Scope
- Backup creation with rotation (`backup-new`, `backup-old`).
- Safe-replace restore with automatic safety snapshot.
- Settings persistence (units, reminders, privacy toggles).

## Dependencies
- M2 complete (required).
- M3 complete (required for full data coverage).
- `docs/01_DOMAIN/BACKUP_RULES.md`
- `docs/02_ARCHITECTURE/STORAGE_STRATEGY.md`

## Step-by-Step Execution
1. Implement backup manifest and artifact metadata handling.
2. Implement rotation workflow for `backup-new`/`backup-old`.
3. Implement restore pipeline with pre-restore snapshot and safe replace.
4. Implement settings persistence and migration behavior.

## Risks
- Backup rotation errors causing artifact loss.
- Restore failures leading to inconsistent dataset state.
- Settings migration regressions across app upgrades.

## Verification
- Round-trip backup/restore integration tests.
- Rotation behavior tests for success/failure cases.
- Corrupted backup failure-path tests.
- Settings persistence tests across restart/update.

## Acceptance Criteria
- Backup rotation behaves exactly as documented.
- Restore is safe-replace only and always creates safety snapshot first.
- Settings remain stable across restart and version updates.

## Exit Deliverables
- Release-safe backup and settings baseline.
- Data portability confidence for MVP delivery.
