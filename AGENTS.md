# AGENTS

## Repository Intent
Native Android repository for a car owner assistant app.
Primary product differentiator: verified mileage as a trusted core feature.

## Product Principles
- Android-first: prioritize native Android UX and architecture.
- Offline-first: core flows must work without network.
- Verified mileage is product-critical, not optional metadata.
- Privacy-first: collect and retain only necessary user data.

## Technical Baseline
- Kotlin + Jetpack Compose
- Navigation Compose
- Room for structured local data
- DataStore for settings
- WorkManager for background jobs
- Coroutines and Flow for async/state streams

## Working Rules
- Documentation before implementation for major features.
- Keep domain logic outside UI composables.
- Record assumptions and tradeoffs in docs.
- Add dependencies only with explicit rationale.

## Definition of Done (Docs Tasks)
1. Scope documented in relevant files.
2. Risks and assumptions are explicit.
3. Verification approach is stated.
4. Open questions are tracked.

## Expected Task Output
1. What changed
2. Files changed
3. Assumptions
4. Verification
5. Remaining risks/open questions
