# Coding Conventions

## Naming and Structure
- Use domain terms consistently.
- Keep one clear responsibility per class/file.
- Favor explicit interfaces at boundaries.

## Kotlin
- Prefer immutable models.
- Use sealed types for finite state.
- Make nullability intentional.

## Compose
- Keep composables focused on rendering.
- Hoist state and keep business logic out of UI layer.
- Use stable UI state models.

## Domain/Data
- Validate invariants in domain/use-case layer.
- Keep repository APIs intent-based.

## Error Handling
- Typed recoverable errors.
- Actionable messages for user-facing failures.

## Testing
- Behavior-oriented test names.
- Cover happy path and failure path for core rules.
