# Bank REST — Full Service & Controller Implementation Plan

## Overview
Complete the bank cards REST API by implementing all service layer logic and controller endpoints. The project already has entities, repositories, DTOs, mappers, exceptions, and utility classes. What's missing: service implementations (CardService, PersonService, CardBlockRequestService, TransferService) and three REST controllers (CardController, PersonController, AdminController).

## Context
- **Stack**: Spring Boot 3.5.16, Java 25, Gradle Kotlin DSL, JPA + Liquibase, H2 (dev/test)
- **No Spring Security** — user explicitly chose to skip auth entirely
- **No auth mechanism** — user identity must be passed as a request parameter (`personId`) since there's no JWT/session
- **Existing conventions**: `@RequiredArgsConstructor` for DI, records for DTOs, MapStruct with `SPRING` component model, `@RestControllerAdvice` for RFC 7807 `ProblemDetail` errors, `@JsonIgnoreProperties(ignoreUnknown = true)` on all DTOs, validation annotations from `jakarta.validation`

---

## 1. New DTOs to Create

### 1.1 `dto/card/CardBlockRequestDto`
```java
package com.example.bankcards.dto.card;

public record CardBlockRequestDto(
    Long id,
    Long cardId,
    Long personId,
    BlockRequestStatus blockRequestStatus,
    Instant createdDate,
    Instant lastModifiedDate
) {}
```

### 1.2 `dto/card/CardBlockRequestCreate`
```java
package com.example.bankcards.dto.card;

public record CardBlockRequestCreate(
    @NotNull Long cardId,
    @NotNull Long personId
) {}
```

### 1.3 `dto/card/CardTransferRequest`
```java
package com.example.bankcards.dto.card;

public record CardTransferRequest(
    @NotNull Long fromCardId,
    @NotNull Long toCardId,
    @NotNull @Positive BigDecimal amount,
    @NotNull Long personId
) {}
```

### 1.4 `dto/card/CardTransferResponse`
```java
package com.example.bankcards.dto.card;

public record CardTransferResponse(
    Long fromCardId,
    Long toCardId,
    BigDecimal amount,
    BigDecimal newFromBalance,
    BigDecimal newToBalance
) {}
```

### 1.5 `dto/person/PersonUpdateRequest`
```java
package com.example.bankcards.dto.person;

public record PersonUpdateRequest(
    @NotBlank String name,
    @NotBlank String password,
    @NotNull Role role
) {}
```

---

## 2. Mappers to Create/Update

### 2.1 `mapper/CardBlockRequestMapper`
```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardBlockRequestMapper {
    CardBlockRequest toEntity(CardBlockRequestCreate create);
    CardBlockRequestDto toDto(CardBlockRequest entity);
    List<CardBlockRequestDto> toDtoList(List<CardBlockRequest> entities);
}
```

---

## 3. Service Layer Design

### 3.1 `PersonService` (modify existing)

| Method | Signature | Logic |
|--------|-----------|-------|
| `getAll` | `Page<PersonDto> getAll(Pageable pageable)` | `personRepository.findAll(pageable)` → map to `PersonDto` |
| `getById` | `PersonDto getById(Long id)` | Find by id or throw `ResourceNotFoundException` |
| `create` | `PersonDto create(PersonCreateRequest request)` | Check `existsByName` → throw `DuplicateResourceException` if taken → save → map |
| `update` | `PersonDto update(Long id, PersonUpdateRequest request)` | Find by id → check name uniqueness if changed → update fields → save → map |
| `delete` | `void delete(Long id)` | Find by id → delete |

### 3.2 `CardService` (new)

Dependencies: `CardRepository`, `CardMapper`, `CardNumberGenerator`, `CardEncryptionUtil`, `CardMaskUtil`, `PersonRepository`

| Method | Signature | Logic |
|--------|-----------|-------|
| `getCardsByPerson` | `Page<CardResponse> getCardsByPerson(Long personId, Pageable pageable)` | Find person → `cardRepository.findByPerson_Id(personId, pageable)` → map |
| `getCardById` | `CardResponse getCardById(Long id)` | Find by id → map |
| `createCard` | `CardResponse createCard(CardCreateRequest request)` | Find person by `request.personId()` → generate card number → encrypt → set `encryptedNumber`, `cardStatus = ACTIVE` → save → map |
| `blockCard` | `CardResponse blockCard(Long id)` | Find card → validate `cardStatus == ACTIVE` (else `InvalidCardOperationException`) → set `BLOCKED` → save → map |
| `activateCard` | `CardResponse activateCard(Long id)` | Find card → validate `cardStatus == BLOCKED` → set `ACTIVE` → save → map |
| `deleteCard` | `void deleteCard(Long id)` | Find by id → delete |

**Validation rules for createCard:**
- `expirationDate` must not be in the past → `InvalidCardOperationException`
- Person must exist → `ResourceNotFoundException`

### 3.3 `CardBlockRequestService` (new)

Dependencies: `CardBlockRequestRepository`, `CardBlockRequestMapper`, `CardRepository`, `PersonRepository`

| Method | Signature | Logic |
|--------|-----------|-------|
| `createRequest` | `CardBlockRequestDto createRequest(CardBlockRequestCreate request)` | Find card → find person → validate card belongs to this person → validate card `ACTIVE` → create request with `PENDING` status → save → map |
| `getPendingRequests` | `Page<CardBlockRequestDto> getPendingRequests(Pageable pageable)` | `findByBlockRequestStatus(PENDING, pageable)` → map |
| `approveRequest` | `CardBlockRequestDto approveRequest(Long requestId)` | Find request → validate `PENDING` → set `APPROVED` → update card `cardStatus = BLOCKED` → save request → map |
| `rejectRequest` | `CardBlockRequestDto rejectRequest(Long requestId)` | Find request → validate `PENDING` → set `REJECTED` → save → map |

### 3.4 `TransferService` (new)

Dependencies: `CardRepository`, `CardMapper`

| Method | Signature | Logic |
|--------|-----------|-------|
| `transfer` | `CardTransferResponse transfer(CardTransferRequest request)` | Validate: both cards exist, belong to same person, neither is `BLOCKED`/`EXPIRED`, `fromCardId != toCardId`, amount > 0, `fromCard.balance >= amount` → debit `fromCard`, credit `toCard` → save both → map response |

**Key**: entire method is `@Transactional`

---

## 4. Controller Layer Design

All controllers use `@RestController` with base path under `/api`. No auth headers needed (no security).

### 4.1 `PersonController`

`@RequestMapping("/api/persons")`

| Method | Endpoint | HTTP | Status | Request | Response | Body |
|--------|----------|------|--------|---------|----------|------|
| `getAllPersons` | `/api/persons` | GET | 200 | `Pageable` (query) | `Page<PersonDto>` | — |
| `getPersonById` | `/api/persons/{id}` | GET | 200 | `@PathVariable Long id` | `PersonDto` | — |
| `createPerson` | `/api/persons` | POST | 201 | `@Valid @RequestBody PersonCreateRequest` | `PersonDto` | — |
| `updatePerson` | `/api/persons/{id}` | PUT | 200 | `@PathVariable Long id`, `@Valid @RequestBody PersonUpdateRequest` | `PersonDto` | — |
| `deletePerson` | `/api/persons/{id}` | DELETE | 204 | `@PathVariable Long id` | void | — |

### 4.2 `CardController`

`@RequestMapping("/api/cards")`

| Method | Endpoint | HTTP | Status | Request | Response | Body |
|--------|----------|------|--------|---------|----------|------|
| `getMyCards` | `/api/cards/person/{personId}` | GET | 200 | `@PathVariable Long personId`, `Pageable` | `Page<CardResponse>` | — |
| `getCardById` | `/api/cards/{id}` | GET | 200 | `@PathVariable Long id` | `CardResponse` | — |
| `createCard` | `/api/cards` | POST | 201 | `@Valid @RequestBody CardCreateRequest` | `CardResponse` | — |
| `blockCard` | `/api/cards/{id}/block` | PATCH | 200 | `@PathVariable Long id` | `CardResponse` | — |
| `activateCard` | `/api/cards/{id}/activate` | PATCH | 200 | `@PathVariable Long id` | `CardResponse` | — |
| `deleteCard` | `/api/cards/{id}` | DELETE | 204 | `@PathVariable Long id` | void | — |
| `transfer` | `/api/cards/transfer` | POST | 200 | `@Valid @RequestBody CardTransferRequest` | `CardTransferResponse` | — |

### 4.3 `CardBlockRequestController`

`@RequestMapping("/api/block-requests")`

| Method | Endpoint | HTTP | Status | Request | Response | Body |
|--------|----------|------|--------|---------|----------|------|
| `createBlockRequest` | `/api/block-requests` | POST | 201 | `@Valid @RequestBody CardBlockRequestCreate` | `CardBlockRequestDto` | — |
| `getPendingRequests` | `/api/block-requests/pending` | GET | 200 | `Pageable` | `Page<CardBlockRequestDto>` | — |
| `approveRequest` | `/api/block-requests/{id}/approve` | PATCH | 200 | `@PathVariable Long id` | `CardBlockRequestDto` | — |
| `rejectRequest` | `/api/block-requests/{id}/reject` | PATCH | 200 | `@PathVariable Long id` | `CardBlockRequestDto` | — |

---

## 5. Test Strategy

### 5.1 Service Tests (`src/test/java/.../service/`)

Each service test class:
- Uses `@ExtendWith(MockitoExtension.class)`
- Mocks all repository/mapper dependencies with `@Mock` / `@InjectMocks`
- **CardServiceTest**: test create (success + person not found + invalid expiration + card blocked), block (success + already blocked + card not found), activate (success + not blocked), delete (success + not found), getCardsByPerson
- **PersonServiceTest**: test create (success + duplicate name), getAll, getById (success + not found), update (success + not found + name conflict), delete (success + not found)
- **CardBlockRequestServiceTest**: test create (success + card not found + person not found + card doesn't belong to person + card already blocked), approve (success + not pending), reject (success + not pending), getPendingRequests
- **TransferServiceTest**: test transfer (success + insufficient funds + card not found + cards belong to different people + card blocked + same card)

### 5.2 Controller Tests (`src/test/java/.../controller/`)

Each controller test class:
- Uses `@WebMvcTest(ControllerClass.class)`
- `@MockBean` for service dependencies
- `MockMvc` for request simulation
- Tests HTTP status codes, response body shape, validation errors (400), not found (404)

**CardControllerTest**:
- `POST /api/cards` → 201 + response body
- `GET /api/cards/{id}` → 200
- `GET /api/cards/{id}` (not found) → 404
- `GET /api/cards/person/{personId}` → 200 + page
- `PATCH /api/cards/{id}/block` → 200
- `PATCH /api/cards/{id}/activate` → 200
- `DELETE /api/cards/{id}` → 204
- `POST /api/cards/transfer` → 200
- `POST /api/cards` (invalid body) → 400

**PersonControllerTest**:
- `GET /api/persons` → 200 + page
- `GET /api/persons/{id}` → 200
- `GET /api/persons/{id}` (not found) → 404
- `POST /api/persons` → 201
- `POST /api/persons` (duplicate) → 409
- `PUT /api/persons/{id}` → 200
- `DELETE /api/persons/{id}` → 204

**CardBlockRequestControllerTest**:
- `POST /api/block-requests` → 201
- `GET /api/block-requests/pending` → 200 + page
- `PATCH /api/block-requests/{id}/approve` → 200
- `PATCH /api/block-requests/{id}/reject` → 200

### 5.3 Test Profile

Create `src/test/resources/application-test.yml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
  liquibase:
    enabled: false
card:
  bin: "400000"
  retry-limit: 10
  encryption:
    secret: "1234567890123456"
```

---

## 6. File-by-File Creation/Modification List

### New Files

| File | Purpose |
|------|---------|
| `src/main/java/.../dto/card/CardBlockRequestDto.java` | Response DTO for block requests |
| `src/main/java/.../dto/card/CardBlockRequestCreate.java` | Request DTO for creating block requests |
| `src/main/java/.../dto/card/CardTransferRequest.java` | Request DTO for card transfers |
| `src/main/java/.../dto/card/CardTransferResponse.java` | Response DTO for transfers |
| `src/main/java/.../dto/person/PersonUpdateRequest.java` | Request DTO for updating persons |
| `src/main/java/.../mapper/CardBlockRequestMapper.java` | MapStruct mapper for block requests |
| `src/main/java/.../service/CardService.java` | Card CRUD + block/activate service |
| `src/main/java/.../service/CardBlockRequestService.java` | Block request workflow service |
| `src/main/java/.../service/TransferService.java` | Card-to-card transfer service |
| `src/main/java/.../controller/PersonController.java` | Person REST endpoints |
| `src/main/java/.../controller/CardController.java` | Card REST endpoints |
| `src/main/java/.../controller/CardBlockRequestController.java` | Block request REST endpoints |
| `src/test/resources/application-test.yml` | H2 test profile config |
| `src/test/java/.../service/CardServiceTest.java` | Unit tests for CardService |
| `src/test/java/.../service/CardBlockRequestServiceTest.java` | Unit tests for CardBlockRequestService |
| `src/test/java/.../service/TransferServiceTest.java` | Unit tests for TransferService |
| `src/test/java/.../controller/PersonControllerTest.java` | MockMvc tests for PersonController |
| `src/test/java/.../controller/CardControllerTest.java` | MockMvc tests for CardController |
| `src/test/java/.../controller/CardBlockRequestControllerTest.java` | MockMvc tests for CardBlockRequestController |

### Modified Files

| File | Change |
|------|--------|
| `src/main/java/.../service/PersonService.java` | Add `getAll`, `getById`, `create`, `update`, `delete` methods |
| `src/main/java/.../repository/CardRepository.java` | Add `findCardsByPerson_IdAndCardStatus` for transfers |

---

## 7. Implementation Order

1. **Test profile** — create `application-test.yml` first so all subsequent tests run
2. **New DTOs** — `CardBlockRequestDto`, `CardBlockRequestCreate`, `CardTransferRequest`, `CardTransferResponse`, `PersonUpdateRequest`
3. **New mapper** — `CardBlockRequestMapper`
4. **PersonService** — implement all methods + `PersonServiceTest`
5. **CardService** — implement all methods + `CardServiceTest`
6. **CardBlockRequestService** — implement all methods + `CardBlockRequestServiceTest`
7. **TransferService** — implement transfer + `TransferServiceTest`
8. **PersonController** — implement endpoints + `PersonControllerTest`
9. **CardController** — implement endpoints + `CardControllerTest`
10. **CardBlockRequestController** — implement endpoints + `CardBlockRequestControllerTest`
11. **Repository update** — add `findCardsByPerson_IdAndCardStatus` if needed by transfer logic
12. **Run full test suite** — verify all pass

---

## Post-Completion

- Run `./gradlew test` to verify all tests pass
- Run `./gradlew build` to verify compilation
- Swagger UI available at `/swagger-ui.html` once endpoints are live
