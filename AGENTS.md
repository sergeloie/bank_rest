# AGENTS.md — Bank Cards REST API

## Project Overview
Spring Boot 3.5.16 + JPA + Liquibase banking card management system. Java 25, Gradle build, PostgreSQL (prod) / H2 (dev & test).

Base package: `com.example.bankcards`
Entry point: `src/main/java/com/example/bankcards/BankRestApplication.java`

## Quick Commands
```bash
# Build and run tests
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.bankcards.controller.CardControllerTest"

# Run tests with coverage report (JaCoCo)
./gradlew jacocoTestReport

# Run app in dev profile (H2 in-memory DB)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Architecture

### Layers
- **Controller** → `controller/` (REST endpoints, `@RestController`, all return DTOs)
- **Service** → `service/` (business logic, `@Service`)
- **Repository** → `repository/` (Spring Data JPA, `CrudRepository`)
- **Entity** → `entity/` (JPA entities: `Card`, `Person`, `CardBlockRequest`)
- **DTO** → `dto/` (request/response records organized by entity subfolder)
- **Mapper** → `mapper/` (MapStruct mappers between entities and DTOs)
- **Config** → `config/` (`CardProperties` for `card.*` properties)
- **Util** → `util/` (card number generation, encryption, masking)
- **Exception** → `exception/` (custom exceptions + `GlobalExceptionHandler`)

### API Endpoints
- `POST/GET /api/cards` — create, list cards by person
- `PATCH /api/cards/{id}/block`, `/activate` — status changes
- `POST /api/cards/transfer` — inter-card transfer (requires `personId` in request)
- `POST/GET /api/persons` — CRUD for persons
- `POST /api/block-requests`, `GET /pending`, `PATCH /{id}/approve|reject` — card block workflow

All endpoints use Spring Data `Pageable` for pagination.

### Database
- Liquibase migrations in `src/main/resources/db/changelog/`, master: `db.changelog-master.yaml`
- **Never modify existing migration files** — add new ones numbered sequentially
- Tables: `person`, `card`, `card_block_request`
- Dev: H2 in-memory (`application-dev.yaml`), Prod: PostgreSQL (`application-prod.yaml`)
- Dev profile uses `ddl-auto: validate`; test profile uses `ddl-auto: create-drop` with Liquibase disabled

### Security
- Spring Security + JWT (currently commented out in `build.gradle.kts` lines 24-25 — security starter is NOT active)
- `security/` package is a placeholder with only a README — no actual security config implemented yet
- Admin user seeded via Liquibase (`04-create-admin.yaml`) using `${BANK_ADMIN_PASSWORD}` env var

### Card System
- Card numbers generated with BIN prefix from `card.bin` property (default `400000`), masked in responses as `**** **** **** XXXX`
- Card numbers encrypted at rest using AES (`CardEncryptionUtil`, secret from `card.encryption.secret`)
- Retry limit for card number generation: `card.retry-limit` (100 in dev, 10 in test)
- `CardStatus` enum: `ACTIVE`, `BLOCKED`, `EXPIRED`

## Environment Variables Required (prod)
- `BANK_ADMIN_PASSWORD` — admin seed password
- `CARD_ENCRYPTION_SECRET` — AES encryption key
- `BANK_JWT_SECRET` — JWT signing secret
- `PG_URL`, `PG_USERNAME`, `PG_PASSWORD` — PostgreSQL connection

## Testing
- Tests use H2 with Liquibase disabled (`application-test.yml`)
- `@WebMvcTest` for controller tests with `@MockitoBean` (Spring Boot 3.4+ style, not `@MockBean`)
- `@MockitoBean(name = "jpaMappingContext")` is required in `@WebMvcTest` to avoid JPA mapping context conflicts
- `LiquibaseMigrationTest` runs against `dev` profile to verify migrations
- Test DTOs are constructed with explicit arguments (records, not builders)

## Conventions
- Controllers return DTOs, never entities directly
- `@RequiredArgsConstructor` for constructor injection
- `@Valid` on all `@RequestBody` parameters
- `ProblemDetail` (RFC 7807) for error responses via `GlobalExceptionHandler`
- MapStruct for entity-DTO mapping (annotation processor configured in `build.gradle.kts`)
- Lombok for boilerplate reduction
- Spring Data Auditing enabled (`@EnableJpaAuditing`) for `createdDate`/`lastModifiedDate`

## Known Gaps
- No actual JWT/security implementation (starter is commented out)
- `docker-compose.yml` is a placeholder — no real compose config
- Several README placeholder files exist in source tree — delete before final build

## Amplicode MCP & Skills

**Always connect to the Amplicode MCP server** before starting any work. Use MCP tools to inspect the project (beans, endpoints, entities, build status) rather than guessing from file names.

**Use all available Java/Spring skills** when they match the task at hand:
- `spring-explore` — initial project context gathering
- `spring-data-jpa` — entity/repository changes
- `spring-security-configuration` — security setup
- `crud-rest-controller` — new CRUD endpoints
- `dto-creator` / `mapper-creator` — DTO and MapStruct mapper creation
- `java-springboot` — general Spring Boot best practices
- `java-junit` — test patterns
- `java-docs` — Javadoc
- `spring-planning` — structured implementation plans

Do not duplicate work that a skill or MCP tool already provides. Prefer these tools over manual inspection where available.
