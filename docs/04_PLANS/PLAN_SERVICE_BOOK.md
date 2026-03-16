# Milestone M4: Service Book and Reminder Engine

## Objective
Implement service timeline and due-state logic with mileage-aware reminder behavior.

## Scope
- Service record lifecycle.
- Due policy by distance/date.
- Reminder scheduling aligned with approved cadence.
- Service-milage anomaly interaction handling.

## Dependencies
- M2 complete (required).
- M3 recommended (for richer timeline context).
- `docs/01_DOMAIN/DATA_MODEL.md`
- `docs/00_PRODUCT/USER_FLOWS.md`

## Step-by-Step Execution
1. Implement service data model and use cases.
2. Implement due-state calculation engine.
3. Implement reminder scheduler and opt-out handling.
4. Integrate service mileage checks with mileage conflict rules.

## Risks
- Rule precedence ambiguity between date and distance due logic.
- Reminder over-notification if cadence is applied incorrectly.

## Verification
- Unit tests for due-state policy combinations.
- Integration tests for mileage/service interaction.
- Reminder cadence scenario tests.

## Acceptance Criteria
- Service due-state outputs are deterministic and reproducible.
- Reminder cadence matches product policy exactly.
- Service mileage violations surface as defined anomalies.

## Exit Deliverables
- Service timeline feature complete for MVP.
- Stable reminder behavior for backup/analytics phases.
