Goal:
Implement the verified mileage domain and UI.

Context:
Read:
- AGENTS.md
- docs/01_DOMAIN/MILEAGE_DOMAIN.md
- docs/01_DOMAIN/DATA_MODEL.md
- docs/02_ARCHITECTURE/*
- docs/03_QUALITY/*
- docs/04_PLANS/PLAN_MILEAGE_CORE.md

Constraints:
- Mileage is a core product feature.
- Store kilometers and miles together.
- Manual entry is required.
- Photo odometer flow must exist.
- Implement trust score, anomaly log, and mileage history.
- Keep logic testable outside UI.

Done when:
- user can add mileage entries
- user can add odometer photo with required manual value
- trust score is visible
- anomaly log is visible
- mileage history works
- tests cover anomaly detection and trust rules

Output format:
1. files changed
2. business rules implemented
3. screens implemented
4. tests added
5. how to verify
6. open risks