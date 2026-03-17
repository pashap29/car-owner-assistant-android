Goal:
Implement the fuel entry module with QR-assisted entry.

Context:
Read the relevant docs, especially FUEL_DOMAIN.md and MILEAGE_DOMAIN.md.

Constraints:
- Fuel is a specialized entry type.
- Support fuel types approved in docs.
- Auto-suggest the most frequently used fuel type from prior records.
- QR scan only pre-fills date and total amount in MVP.
- User enters the new odometer value; app computes delta.
- Fuel and mileage domains must remain consistent.

Done when:
- user can add fuel entries
- QR-assisted flow works
- fuel type auto-suggestion works
- mileage impact is recorded
- tests cover fuel defaults and mileage integration

Output format:
1. files changed
2. implemented flows
3. tests added
4. verification steps
5. risks