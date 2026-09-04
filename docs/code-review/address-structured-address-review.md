# Code Review: Structured Address Support

**Reviewed**: 2026-09-04  
**Scope**: Uncommitted local changes for user-service structured address support  
**Decision**: REQUEST CHANGES

## Summary

The structured-address fields, DTO mappings, migration, and focused service tests are in place. Two input-validation gaps can cause invalid or oversized user input to be stored or to fail as database errors.

## Findings

### HIGH

**`service/user/server/src/main/java/com/omisys/user/presentation/request/AddressRequest.java:29`**  
**`service/user/server/src/main/java/com/omisys/user/domain/model/Address.java:83`**

Issue: Requests that omit both `address` and `roadAddress` are accepted. `resolveAddress` then produces an empty string for the required legacy `address` field, despite the request comment stating that `roadAddress` is required when `address` is omitted.

Fix: Add class-level validation to both request DTOs that requires at least one non-blank address source.

### HIGH

**`service/user/server/src/main/java/com/omisys/user/presentation/request/AddressRequest.java:32`**  
**`service/user/server/src/main/java/com/omisys/user/domain/model/Address.java:83`**

Issue: `roadAddress` can be 255 characters and `detailAddress` can be 100 characters, but the derived legacy `address` column uses JPA's default 255-character length. Valid input can exceed the column limit and produce a database truncation error. `sido` and `sigungu` also lack validation matching their 40-character migration columns.

Fix: Validate each component and the combined legacy-address length, or increase the legacy column size in a migration. Add `@Size(max = 40)` to `sido` and `sigungu`.

## Validation

| Check | Result |
|---|---|
| `git diff --check HEAD` | Pass |
| `:service:user:server:test --tests com.omisys.user.application.service.AddressServiceTest` | Pass (13 seconds) |
| Full module tests | Not run; the implementation report records an unresolved external config-server dependency |

## Files Reviewed

- 10 tracked changed files
- `docs/migrations/user/20260903-address-structured-columns.sql`
- `.agents/PRPs/reports/address-search-backend-report.md`
