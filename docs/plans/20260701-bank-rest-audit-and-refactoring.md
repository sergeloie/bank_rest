# Implementation Plan - Bank Rest Audit and Refactoring

## Overview
This plan outlines a comprehensive audit and refactoring of the `bank_rest` Spring Boot application. The main goal is to identify security vulnerabilities, business logic issues, database inefficiencies, exception handling gaps, and test quality deficiencies, and then fix them systematically.

The execution follows the **Audit-First** approach: we will first perform a full read-only audit of the codebase, write reproducing test cases (TDD) for every issue, compile a detailed Audit Report, and then implement the refactoring steps starting from the most critical issues to the least critical ones.

## Context (from discovery)
- **Spring Boot Version:** 3.5.16
- **Language:** Java
- **Build System:** Gradle (Kotlin DSL: `build.gradle.kts`)
- **Main Module:** `bank_rest`
- **Key Stack:** Spring Security + JWT, Spring Data JPA, Liquibase, PostgreSQL, Lombok, MapStruct, JUnit 5.
- **Related Files:**
  - Security Config: `SecurityConfig.java`, `JwtTokenProvider.java`, `JwtAuthFilter.java`
  - Controllers: `AuthController.java`, `CardController.java`, `PersonController.java`, `TransferController.java`, etc.
  - Services: `AuthService.java`, `CardService.java`, `PersonService.java`, `TransferService.java`, `CardBlockRequestService.java`
  - Entities: `Person.java`, `Card.java`, `CardBlockRequest.java`
  - Migrations: `src/main/resources/db/migration/*.yaml`

## Development Approach
- **Testing Approach:** **TDD / Test-First**. We will write failing/reproducing tests for every vulnerability, bug, or validation issue before applying the fix, and then verify the tests pass.
- **Complexity Guideline:** Keep the code as simple as possible (KISS, YAGNI). Avoid unnecessary abstractions, design patterns, or layers. Each change must make the code simpler, safer, or more correct.
- **Concurrency & Locking:** Use deterministic pessimistic locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on card updates during transfers to prevent deadlocks and race conditions.
- **JPA Best Practices:** Enforce `FetchType.LAZY` on relationships. Avoid `CascadeType.ALL` (use selective cascades). Address the N+1 problem using `JOIN FETCH` or `@EntityGraph`.
- **Audit Logging & Security:** Protect sensitive data (like unmasked card numbers or plain text passwords) from entering the logs.

## Testing Strategy
- **Unit Tests:** Used for isolating service business logic, utility methods, and encoders/mappers.
- **Integration Tests:** Used for verifying API security rules, access matrices, horizontal/vertical privilege escalation, validation constraints, and database transactional rollbacks. Uses Spring Boot context + H2 test database.
- **Failure Replication:** For every security vulnerability or logic error, a test case MUST be created that fails before the fix and passes after the fix.

## Progress Tracking
- `[ ]` - Not started
- `[x]` - Completed
- `[+]` - Added during implementation
- `[!]` - Blocked / Issue documented

---

## Solution Overview
We will execute the work in two main phases:
1. **PHASE 1: Full Read-Only Audit & Bug Replication (Stages 1 to 8)**
   - Read and analyze every aspect of the project.
   - Write failing integration and unit tests reproducing every security, validation, database, and logic flaw we find.
   - Compile a detailed Audit Report identifying the exact files, lines, and test cases that reproduce the issues.
2. **PHASE 2: Sequential Refactoring (Stage 9)**
   - Fix all CRITICAL issues (Security breaches, race conditions, horizontal/vertical escalation, missing checks).
   - Fix all IMPORTANT issues (N+1 queries, transaction boundaries, optimistic locking/versioning, input validation).
   - Fix all IMPROVEMENT issues (Naming conventions, MapStruct mappers, config cleanup, unused dependencies, log leaks).

---

## Technical Details
- **Access Matrix Verification:**
  - `ADMIN`: Allowed to manage users, cards (create, activate, block, delete). Not allowed to perform transfers.
  - `USER`: Allowed to view only their own cards, request blocking of only their own cards, and transfer money only between their own cards.
  - Public: Auth login, OpenAPI / Swagger UI.
- **Card Security:**
  - Card number encrypted in DB via AES-256 (handled in service/utility layer, not DB).
  - Card number masked in API output (`**** **** **** 1234`).
- **Database Safety:**
  - Add `@Version` for optimistic locking or use pessimistic lock on cards during transfer.
  - Handle numeric precision with `BigDecimal` and mapping to `NUMERIC(19,2)`.

---

## What Goes Where
The audit and refactoring steps are achievable entirely within the `bank_rest` codebase:
- Tests go to `src/test/java/...`
- Logic goes to `src/main/java/...`
- Database migrations go to `src/main/resources/db/migration/`

---

## Implementation Steps

### Phase 1: Comprehensive Read-Only Audit & Test Creation

#### Task 1: Project Structure & Requirement Exploration
**Files:**
- Modify: `docs/plans/20260701-bank-rest-audit-and-refactoring.md`

- [ ] Read README.md, README_Bank_rest.md, and inspect package/module structure
- [ ] Read build.gradle.kts, application.yml, and db migrations
- [ ] Inspect all JPA Entity mappings, relationships, and Liquibase schema definitions
- [ ] Inspect Spring Security configurations, filter chain, and CORS/CSRF setup
- [ ] Inspect all controllers, services, repositories, and current test suite
- [ ] Compile initial project state assessment summary in memory/notes

#### Task 2: Security & Authorization Audit (with Failing Tests)
**Files:**
- Create/Modify: `src/test/java/com/example/bankcards/integration/SecurityAuditTest.java`

- [x] Analyze controllers and filters for horizontal privilege escalation (USER accessing another USER's data)
- [x] Analyze controllers and filters for vertical privilege escalation (USER accessing ADMIN endpoints)
- [x] Analyze ADMIN operations for unsafe behavior (ADMIN doing transfers, ADMIN seeing plain card numbers)
- [x] Write reproducing failing tests for horizontal privilege vulnerabilities
- [x] Write reproducing failing tests for vertical privilege vulnerabilities
- [x] Write reproducing failing tests for ADMIN-only constraint violations
- [x] Run test suite to verify tests actually fail on the current codebase

#### Task 3: Database & JPA Mapping Audit
**Files:**
- Modify: `docs/plans/20260701-bank-rest-audit-and-refactoring.md` (add findings)

- [x] Identify N+1 query problems in all list-fetching endpoints (users, cards, block-requests)
- [x] Examine relationship mappings (LAZY vs EAGER) and Cascade types in all entities
- [x] Verify index presence on all search and filter columns (e.g., card status, owner, email)
- [x] Inspect primary key data types and database field types (e.g., numeric precision for balances)
- [x] Document all JPA / DB schema deviations and list them in the Audit Report

#### Task 4: Business Logic & Service Layer Audit (with Failing Tests)
**Files:**
- Create/Modify: `src/test/java/com/example/bankcards/integration/BusinessLogicAuditTest.java`

- [x] Verify transfer logic: balance checks, transfer to same card, negative/zero amounts
- [x] Verify transfer logic: transfer from/to expired cards, transfer from/to blocked cards
- [x] Verify race conditions: write a multi-threaded test for concurrent transfers on the same card (verify if balance corrupts or deadlocks occur due to lack of pessimistic lock)
- [x] Verify card number encryption/decryption and masking flow
- [x] Run test suite to ensure the new business logic failure tests fail as expected

#### Task 5: Exception Handling & Validation Audit
**Files:**
- Create/Modify: `src/test/java/com/example/bankcards/integration/ExceptionHandlingAuditTest.java`

- [x] Check `@RestControllerAdvice` and error formats: ensure no stack traces leak in HTTP responses
- [x] Check inputs validation: `@Valid` and constraints (`@NotNull`, `@Size`, `@Positive`)
- [x] Check exception mapping codes (404 for missing entities, 403 for AccessDenied, 400/422 for logic errors, 409 for conflicts)
- [x] Write failing validation/exception test cases to verify current deficiencies in response structure

#### Task 6: Compile Full Audit Report
**Files:**
- Create: `docs/AUDIT_REPORT.md`

- [x] List all found issues categorized by severity: CRITICAL, IMPORTANT, IMPROVEMENT
- [x] Reference exact files, lines, and corresponding reproducing test cases
- [x] Obtain user review and confirmation before starting Phase 2

---

### Phase 2: Refactoring & Verification (TDD Execution)

#### Task 7: Fix CRITICAL Security & Race Condition Issues
> **JPA skill required (spring-data-jpa).** Before writing any entity/service code, activate the skill and follow its rules.

**Files:**
- Modify: `src/main/java/com/example/bankcards/security/SecurityConfig.java` (and related security classes)
- Modify: `src/main/java/com/example/bankcards/service/TransferService.java`
- Modify: `src/main/java/com/example/bankcards/service/CardService.java`
- Modify: `src/main/java/com/example/bankcards/service/PersonService.java`

- [x] Implement proper URL-based and method-level security checks to block vertical escalation
- [x] Enforce owner-checks in service layer to prevent horizontal privilege escalation
- [x] Add pessimistic lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) in `CardRepository` and sort locks in transfer to prevent race conditions and deadlocks
- [x] Stop ADMIN from accessing USER transfers, and ensure plain text card numbers are never sent to ADMIN/USER
- [x] Run security and transfer integration tests - must pass completely before proceeding

#### Task 8: Fix IMPORTANT Database & Validation Issues
> **JPA skill required (spring-data-jpa).**

**Files:**
- Modify: `src/main/java/com/example/bankcards/entity/Person.java`
- Modify: `src/main/java/com/example/bankcards/entity/Card.java`
- Modify: `src/main/java/com/example/bankcards/repository/` (add JPA EntityGraphs / JOIN FETCH)
- Create: `src/main/resources/db/migration/07-add-missing-indexes-and-optimistic-locking.yaml`

- [x] Convert all entity relationships to strict `FetchType.LAZY` and use `@EntityGraph` or `JOIN FETCH` to resolve N+1 queries
- [x] Add `@Version` optimistic locking to Person and Card entities if applicable
- [x] Create a new Liquibase migration to add missing indexes on filter/search columns and adjust column types if needed
- [x] Add comprehensive `@Valid` annotations on controllers and appropriate Bean Validation annotations on DTOs
- [x] Run JPA, DB, and validation integration tests - must pass completely before proceeding

#### Task 9: Fix IMPROVEMENT Code Quality & Exception Handling Issues
**Files:**
- Modify: `src/main/java/com/example/bankcards/exception/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/example/bankcards/mapper/`
- Modify: `src/main/resources/application.yml`

- [x] Refactor `GlobalExceptionHandler` to return consistent API error structure without exposing stack traces
- [x] Clean up configuration: load sensitive values (JWT secret, DB password) from environment variables
- [ ] Standardize MapStruct mappers, ensuring entities are never returned from controllers and DTOs are clean
- [ ] Remove redundant/excessive abstractions or unused classes in the codebase
- [x] Run entire test suite - all tests must pass

#### Task 10: Final Acceptance & Plan Verification
**Files:**
- Modify: `docs/plans/20260701-bank-rest-audit-and-refactoring.md`

- [x] Verify all acceptance criteria from the audit are met
- [x] Ensure full test coverage on success, error, and edge cases
- [x] Rebuild project and verify compilation using `./gradlew build` or IDE rebuild
- [x] Move this plan to `docs/plans/completed/`

---

## Post-Completion
*Manual and external verification steps (no checkboxes)*

**Manual validation:**
- Start docker-compose and test Swagger endpoints with token login.
- Verify security responses manually via `Postman` or `curl`.
- Analyze log outputs to confirm no sensitive information is logged.
