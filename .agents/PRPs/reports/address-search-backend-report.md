# Implementation Report: Address Search Backend

## Summary

Implemented structured address support for user-service. Address requests can now carry road, parcel, detail, province, and district components; legacy `address` remains supported and is derived from road plus detail when omitted.

## Tasks Completed

| # | Task | Status | Notes |
|---|---|---|---|
| 1 | Address entity | Complete | Added five nullable fields and legacy address composition. |
| 2 | Request validation | Complete | Added required-field, zip code, phone, and detail validations. |
| 3 | Controller validation | Complete | Added `@Valid` to POST and an ApiResponse validation handler. |
| 4 | Response/shared DTO | Complete | Added structured mappings, including internal DTO mapping. |
| 5 | Migration | Complete | Added nullable `p_address` columns and deployment documentation. |
| 6 | Service tests | Complete | Added four structured-address regression tests. |
| 7 | OpenAPI generation | Blocked | user-service was not available on `localhost:18081`. |

## Validation Results

| Level | Status | Notes |
|---|---|---|
| Static analysis | Pass | `:service:user:server:compileJava` completed successfully. |
| Focused unit tests | Pass | `AddressServiceTest` completed successfully. |
| Full module tests | Blocked | `UserApplicationTests.contextLoads` fails before application tests because the required external config-server host cannot resolve. |
| Build | Pass | Compile task completed successfully. |
| OpenAPI generation | Blocked | `python scripts/api/dump_openapi.py user` could not connect to localhost port 18081. |
| Diff whitespace check | Pass | `git diff --check` passed. |

## Files Changed

| File | Action |
|---|---|
| `service/user/server/.../Address.java` | Updated |
| `service/user/server/.../AddressRequest.java` | Updated |
| `service/user/server/.../AddressController.java` | Updated |
| `service/user/server/.../AddressResponse.java` | Updated |
| `service/user/server/.../AddressInternalService.java` | Updated |
| `service/user/server/.../UserControllerAdvice.java` | Updated |
| `service/user/user_dto/.../AddressDto.java` | Updated |
| `service/user/server/.../AddressServiceTest.java` | Updated |
| `docs/migrations/user/20260903-address-structured-columns.sql` | Created |
| `docs/migrations/README.md` | Updated |

## Deviations from Plan

- Added `UserControllerAdvice` handling for `MethodArgumentNotValidException`: user-service did not previously convert validation failures into the required `ApiResponse` envelope.
- Updated `AddressInternalService` because `AddressDto` is consumed by order-service and its constructor needed the appended fields.
- OpenAPI output was not regenerated because the required local user-service was unavailable.

## Tests Written

| Test | Coverage |
|---|---|
| `createAddress_structuredFields_persisted` | All five structured fields persist. |
| `createAddress_composesAddressWhenAbsent` | Road/detail compose legacy address. |
| `createAddress_keepsExplicitAddress` | Explicit legacy address takes precedence. |
| `updateAddress_structuredFields_updated` | Updates replace all structured fields. |

## Next Steps

- Start user-service with its config dependency available, then run `python scripts/api/dump_openapi.py user` and commit `docs/api/user.json`.
- Resolve or make optional the config-server dependency for `UserApplicationTests.contextLoads`, then run `:service:user:server:test`.
