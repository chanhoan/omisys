# Local Uncommitted Change Review - Queue Status

**Reviewed**: 2026-09-02
**Scope**: Local changes relative to `HEAD`
**Decision**: REQUEST CHANGES

## Summary

The change adds a queue-status endpoint, polling metadata, and expiry of inactive waiting users. The filter-level behavior compiles and its focused tests pass, but the new cleanup has a race that can evict a user who has just refreshed their waiting activity.

## Findings

### CRITICAL

None.

### HIGH

**[HIGH]** `service/gateway/server/src/main/java/com/omisys/gateway/server/application/UserQueueService.java:341`

Issue: Expiry is implemented as separate Redis reads and deletes. The scheduler can read an old activity score, then a concurrent `GET /api/queue/status` refreshes the score at line 169, after which the scheduler still removes that user's wait and activity entries at lines 346-347. A user polling at the inactivity boundary can therefore receive `WAITING` and then be incorrectly changed to `EXPIRED`, losing their queue position.

Fix: Make the stale-score check and removal atomic in Redis (for example, a Lua script that reads the activity score, compares it with the cutoff, and removes both ZSET entries only when it is still stale). Add a concurrent/interleaving test proving a refreshed entry is retained.

### MEDIUM

**[MEDIUM]** `service/gateway/server/src/main/java/com/omisys/gateway/server/application/UserQueueService.java:260`

Issue: Adding a waiting user and adding its activity record are separate writes. If the second command fails after line 263 succeeds, the user stays in the wait ZSET without an activity entry. The new cleanup cannot expire that user, so an abandoned queue entry can later be promoted and consume an active-user slot.

Fix: Atomically create/update the wait and activity entries (Lua script or a transaction with a defined recovery path), or compensate by removing the wait entry if the activity write fails. Test the partial-failure path.

**[MEDIUM]** `service/gateway/server/src/main/java/com/omisys/gateway/server/application/UserQueueService.java:158`

Issue: No `UserQueueService` tests cover the newly introduced state transitions: waiting activity refresh, inactivity expiry, promotion removing the activity entry, retry interval rounding, or Redis write failures. The existing filter tests only mock `getQueueStatus`, so they cannot detect queue-storage regressions.

Fix: Add focused service tests with a Redis test double/Testcontainers Redis for each transition, including the two failure/concurrency cases above.

### LOW

None.

## Validation Results

| Check | Result | Notes |
|---|---|---|
| Diff whitespace | Pass | `git diff --check` produced no errors. |
| Focused gateway filter tests | Pass | `:service:gateway:server:test --tests com.omisys.gateway.server.infrastructure.filter.GlobalQueueFilterTest` completed successfully. |
| Queue-service integration/concurrency tests | Skipped | No such tests are present in the changed scope. |

## Files Reviewed

- `service/gateway/server/src/main/java/com/omisys/gateway/server/application/UserQueueService.java` - queue state, expiry, scheduling
- `service/gateway/server/src/main/java/com/omisys/gateway/server/application/dto/QueueState.java` - response state enum
- `service/gateway/server/src/main/java/com/omisys/gateway/server/application/dto/QueueStatusResponse.java` - status DTO
- `service/gateway/server/src/main/java/com/omisys/gateway/server/infrastructure/filter/GlobalQueueFilter.java` - queue-status endpoint and HTTP response
- `service/gateway/server/src/test/java/com/omisys/gateway/server/infrastructure/filter/GlobalQueueFilterTest.java` - filter behavior tests
