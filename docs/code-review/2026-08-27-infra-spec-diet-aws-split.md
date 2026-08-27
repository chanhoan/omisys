# Local Code Review: infra-spec-diet-aws-split

**Reviewed**: 2026-08-27  
**Scope**: Local uncommitted changes against `HEAD`  
**Plan**: `.claude/PRPs/plans/infra-spec-diet-aws-split.plan.md`  
**Decision**: REQUEST CHANGES

## Summary

The infrastructure reduction and Product Cassandra-to-JPA migration contain blocking deployment and developer-workflow issues. The Product service test suite passes, but the SSH tunnel scripts do not parse, MySQL users are initialized with literal placeholder passwords, and several implementation-critical files remain untracked.

## Findings

### HIGH

**[HIGH]** `scripts/tunnel.ps1:33`, `scripts/tunnel.sh:38`  
Issue: Both tunnel scripts have an unterminated double-quoted status message. The PowerShell script fails parsing before it can create the SSH tunnel; the Bash script has the equivalent syntax defect.  
Fix: Close both strings, then validate with PowerShell script parsing and `bash -n`.

**[HIGH]** `docker-compose-dep.yml:207`, `docs/migrations/mysql/init-schemas.sql:26`  
Issue: Docker Compose mounts the template SQL directly into MySQL's automatic initialization directory, but no process substitutes `:USER_PW`, `:PRODUCT_PW`, and the remaining placeholders. On an empty volume MySQL creates accounts whose passwords are literally those placeholder values, so applications using their real environment passwords cannot authenticate.  
Fix: Generate a substituted SQL file before deployment and mount that generated artifact, or add a secure MySQL initialization script that reads the required environment variables and creates the accounts without committing secrets.

**[HIGH]** `service/product/server/src/main/java/com/omisys/product/application/product/ProductService.java:6`  
Issue: The new JPA `ProductRepository`, MySQL initialization SQL, and tunnel scripts are untracked. Committing only the staged change set leaves Product imports unresolved and omits the Compose-mounted initialization artifact.  
Fix: Explicitly add all required new files to the intended commit. If an artifact is intentionally excluded, remove every dependency on it from this change set.

### MEDIUM

**[MEDIUM]** `service/product/server/src/main/java/com/omisys/product/application/product/ProductService.java:98`  
Issue: The new list implementation now preserves repository `totalElements` and accepts null price filters, but no test covers either behavior. The implementation plan explicitly calls for total-count, null-filter, and all-filter tests.  
Fix: Add tests that return a multi-page `Page<Product>` and assert total elements, mapped content, and null/complete filter parameter handling.

**[MEDIUM]** `docs/migrations/README.md:5`, `docs/migrations/README.md:14`  
Issue: The documentation says the initializer creates seven schemas and omits `:DELIVERY_PW`, while the SQL creates eight schemas/accounts including delivery.  
Fix: Update the schema count and list every required placeholder, including `:DELIVERY_PW`.

### LOW

**[LOW]** Working tree formatting  
Issue: `git diff --check HEAD` reports trailing whitespace, and the staged line-ending normalization expands the review to 304 changed files even though `git diff -w` identifies 38 semantic changes.  
Fix: Remove unintended trailing whitespace and commit `.gitattributes` with the normalization in a focused commit before or alongside the semantic changes.

## Validation Results

| Check | Result |
|---|---|
| Product service tests | Pass — `./gradlew.bat :service:product:server:test --rerun-tasks` |
| PowerShell tunnel syntax | Fail — unterminated string |
| Bash tunnel syntax | Skipped — Bash executable unavailable in the review environment; equivalent unterminated string identified by inspection |
| Git whitespace check | Fail — trailing whitespace reported |
| Docker Compose config | Skipped — Docker CLI unavailable in the review environment |

## Files Reviewed

- Infrastructure: `docker-compose*.yml`, monitoring workflow, removed ELK configuration, JVM/memory settings.
- Product migration: Product entity/service/repository configuration, Cassandra repository removal, messaging consumer, and related tests.
- Operational artifacts: MySQL initialization SQL, migration/local-development documentation, `.gitattributes`, and SSH tunnel scripts.
- The complete local scope comprises 304 tracked file changes plus untracked migration, tunnel, and repository artifacts; semantic review prioritized the 38 non-whitespace tracked changes and all untracked artifacts.
