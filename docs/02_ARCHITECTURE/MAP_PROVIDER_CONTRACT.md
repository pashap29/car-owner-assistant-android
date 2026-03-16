# Map Provider Contract

## 1. Purpose
Provide an abstract provider boundary for optional map/place features without coupling domain logic to a specific SDK.

## 2. MVP Scope
- Optional place lookup for fuel/service entry context.
- Optional map preview for selected coordinates.
- Manual fallback when permission/network/provider is unavailable.

Map features are non-blocking for core MVP workflows.

## 3. Contract Interfaces (Conceptual)
- `MapPreviewGateway`
  - render preview by center/zoom/markers
- `PlaceSearchGateway`
  - search places by query + optional bounds
- `PlaceDetailsGateway`
  - fetch normalized place details by provider place id
- `ReverseGeocodeGateway`
  - resolve coordinates to display address

## 4. Normalized Data Models
- `PlaceRef`
  - `providerKey`
  - `providerPlaceId`
  - `normalizedPlaceId`
- `PlaceInfo`
  - display name
  - formatted address
  - coordinates
  - precision/confidence metadata

## 5. Failure Contract
Gateways must return explicit result states:
- `Success`
- `NoNetwork`
- `PermissionDenied`
- `ProviderUnavailable`
- `InvalidRequest`

UI fallback behavior:
- allow manual address entry,
- keep save flow unblocked,
- avoid blocking dialogs without recovery action.

## 6. Privacy Rules
- Request location permission only inside relevant flow.
- Provide rationale before permission prompt.
- Do not retain fine-grained location data if user disabled location retention.

## 7. Feature Flag and Rollout
- Controlled by `map_places_enabled` flag.
- If disabled, app must hide map UI and keep manual location input.

## 8. Future Compatibility
- Provider-specific SDK types must not escape adapter layer.
- Contracts are stable enough to plug future providers without domain/UI rewrites.
