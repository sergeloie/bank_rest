package com.example.bankcards.integration;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.card.CardTransferResponse;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestAdminResponse;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
import com.example.bankcards.dto.person.PersonAdminResponse;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PersonRepository personRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String aliceToken;
    private Long aliceId;
    private Long bobId;
    private final List<Long> aliceCardIds = new ArrayList<>();
    private final List<Long> bobCardIds = new ArrayList<>();

    @BeforeAll
    void setupAuth() {
        // Migration inserts admin with plaintext password — hash it for BCrypt login
        String hashed = passwordEncoder.encode("testadmin");
        jdbcTemplate.update("UPDATE person SET password = ? WHERE name = 'admin'", hashed);

        // Login as admin to get JWT
        var loginReq = new AuthRequest("admin", "testadmin");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        adminToken = loginResp.getBody().accessToken();
        assertNotNull(adminToken);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(adminToken);
        return h;
    }

    private HttpHeaders userAuthHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(aliceToken);
        return h;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private Long extractIdFromResponse(ResponseEntity<?> response) {
        Object body = response.getBody();
        if (body instanceof java.util.Map<?, ?> map) {
            return ((Number) map.get("id")).longValue();
        }
        throw new RuntimeException("Cannot extract id from response: " + body);
    }

    // ==================== PHASE 1: Person CRUD ====================

    @Test
    @Order(1)
    void createAlice() {
        var request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        HttpEntity<PersonCreateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        aliceId = personRepository.findByName("Alice").orElseThrow().getId();

        // Login as Alice to get user token
        var loginReq = new AuthRequest("Alice", "pass123");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        aliceToken = loginResp.getBody().accessToken();
    }

    @Test
    @Order(2)
    void createBob() {
        var request = new PersonCreateRequest("Bob", "pass456", Role.USER);
        HttpEntity<PersonCreateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        bobId = personRepository.findByName("Bob").orElseThrow().getId();
    }

    @Test
    @Order(3)
    void createDuplicate_shouldReturn409() {
        var request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        HttpEntity<PersonCreateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    @Order(4)
    void createWithEmptyName_shouldReturn400() {
        var request = new PersonCreateRequest("", "pass", Role.USER);
        HttpEntity<PersonCreateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(5)
    void getAlice_shouldReturn200() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + aliceId, HttpMethod.GET, entity, Void.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(6)
    void listPersons_shouldReturn200() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons?page=0&size=10", HttpMethod.GET, entity, Void.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(7)
    void updateAlicePasswordOnly() {
        var request = new PersonUpdateRequest("newpass", null);
        HttpEntity<PersonUpdateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + aliceId, HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(8)
    void updateAliceRoleOnly() {
        var request = new PersonUpdateRequest(null, Role.ADMIN);
        HttpEntity<PersonUpdateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + aliceId, HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(9)
    void updateBothNull_shouldReturn400() {
        var request = new PersonUpdateRequest(null, null);
        HttpEntity<PersonUpdateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + aliceId, HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(10)
    void updateNonExistent_shouldReturn404() {
        var request = new PersonUpdateRequest("pass", Role.USER);
        HttpEntity<PersonUpdateRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/99999", HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ==================== PHASE 2: Card Creation ====================

    @Test
    @Order(11)
    void createAliceCards() {
        long[] balances = {100, 500, 1000, 2500, 5000};
        for (long bal : balances) {
            var req = new CardCreateRequest(aliceId, LocalDate.now().plusYears(1), BigDecimal.valueOf(bal));
            HttpEntity<CardCreateRequest> entity = new HttpEntity<>(req, authHeaders());
            ResponseEntity<CardAdminResponse> resp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, entity, CardAdminResponse.class);
            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
            aliceCardIds.add(resp.getBody().id());
        }
        assertEquals(5, aliceCardIds.size());
    }

    @Test
    @Order(12)
    void createBobCards() {
        long[] balances = {200, 800, 1500, 3000, 10000};
        for (long bal : balances) {
            var req = new CardCreateRequest(bobId, LocalDate.now().plusYears(1), BigDecimal.valueOf(bal));
            HttpEntity<CardCreateRequest> entity = new HttpEntity<>(req, authHeaders());
            ResponseEntity<CardAdminResponse> resp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, entity, CardAdminResponse.class);
            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
            bobCardIds.add(resp.getBody().id());
        }
        assertEquals(5, bobCardIds.size());
    }

    @Test
    @Order(13)
    void createCardPastExpiration_shouldReturn400() {
        var req = new CardCreateRequest(aliceId, LocalDate.now().minusDays(1), BigDecimal.ZERO);
        HttpEntity<CardCreateRequest> entity = new HttpEntity<>(req, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(14)
    void createCardForNonExistentPerson_shouldReturn404() {
        var req = new CardCreateRequest(99999L, LocalDate.now().plusYears(1), BigDecimal.ZERO);
        HttpEntity<CardCreateRequest> entity = new HttpEntity<>(req, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @Order(15)
    void listAliceCards_shouldReturn5() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/admin/cards?personId=" + aliceId + "&page=0&size=10", HttpMethod.GET, entity, Void.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(16)
    void getCardById_shouldReturn200() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<CardAdminResponse> resp = restTemplate.exchange(
                "/api/admin/cards/" + aliceCardIds.get(0), HttpMethod.GET, entity, CardAdminResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().maskedNumber().startsWith("**** **** **** "));
        assertEquals(CardStatus.ACTIVE, resp.getBody().cardStatus());
    }

    // ==================== PHASE 3: Card Status ====================

    @Test
    @Order(17)
    void blockCard_shouldReturn200() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<CardAdminResponse> resp = restTemplate.exchange(
                "/api/admin/cards/" + aliceCardIds.get(0) + "/block", HttpMethod.PUT, entity, CardAdminResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(CardStatus.BLOCKED, resp.getBody().cardStatus());
    }

    @Test
    @Order(18)
    void blockAlreadyBlocked_shouldReturn400() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/admin/cards/" + aliceCardIds.get(0) + "/block", HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(19)
    void activateCard_shouldReturn200() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<CardAdminResponse> resp = restTemplate.exchange(
                "/api/admin/cards/" + aliceCardIds.get(0) + "/activate", HttpMethod.PUT, entity, CardAdminResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(CardStatus.ACTIVE, resp.getBody().cardStatus());
    }

    @Test
    @Order(20)
    void activateAlreadyActive_shouldReturn400() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/admin/cards/" + aliceCardIds.get(0) + "/activate", HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(21)
    void blockNonExistent_shouldReturn404() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/admin/cards/99999/block", HttpMethod.PUT, entity, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ==================== PHASE 4: Transfers ====================

    @Test
    @Order(22)
    void transfer_card1ToCard2() {
        var req = new CardTransferRequest(aliceCardIds.get(0), aliceCardIds.get(1), BigDecimal.valueOf(100), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<CardTransferResponse> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, CardTransferResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().newFromBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, resp.getBody().newToBalance().compareTo(BigDecimal.valueOf(600)));
    }

    @Test
    @Order(23)
    void transfer_card2ToCard3() {
        var req = new CardTransferRequest(aliceCardIds.get(1), aliceCardIds.get(2), BigDecimal.valueOf(50), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<CardTransferResponse> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, CardTransferResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(0, resp.getBody().newFromBalance().compareTo(BigDecimal.valueOf(550)));
        assertEquals(0, resp.getBody().newToBalance().compareTo(BigDecimal.valueOf(1050)));
    }

    @Test
    @Order(24)
    void transfer_insufficientFunds_shouldReturn400() {
        var req = new CardTransferRequest(aliceCardIds.get(0), aliceCardIds.get(1), BigDecimal.valueOf(100), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(25)
    void transfer_sameCard_shouldReturn400() {
        Long card = aliceCardIds.get(1);
        var req = new CardTransferRequest(card, card, BigDecimal.valueOf(10), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(26)
    void transfer_differentOwners_shouldReturn400() {
        var req = new CardTransferRequest(aliceCardIds.get(1), bobCardIds.get(0), BigDecimal.valueOf(10), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(27)
    void transfer_wrongPersonId_shouldReturn400() {
        var req = new CardTransferRequest(aliceCardIds.get(1), aliceCardIds.get(2), BigDecimal.valueOf(10), bobId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(28)
    void transfer_fromBlockedCard_shouldReturn400() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(2) + "/block", HttpMethod.PUT, h, Void.class);
        var req = new CardTransferRequest(aliceCardIds.get(2), aliceCardIds.get(3), BigDecimal.valueOf(10), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(2) + "/activate", HttpMethod.PUT, h, Void.class);
    }

    @Test
    @Order(29)
    void transfer_toBlockedCard_shouldReturn400() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(3) + "/block", HttpMethod.PUT, h, Void.class);
        var req = new CardTransferRequest(aliceCardIds.get(1), aliceCardIds.get(3), BigDecimal.valueOf(10), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(3) + "/activate", HttpMethod.PUT, h, Void.class);
    }

    @Test
    @Order(30)
    void transfer_nonExistentCard_shouldReturn404() {
        var req = new CardTransferRequest(99999L, aliceCardIds.get(1), BigDecimal.valueOf(10), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @Order(31)
    void transfer_nullFields_shouldReturn400() {
        HttpEntity<String> entity = new HttpEntity<>("{}", userAuthHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ==================== PHASE 5: Block Requests ====================

    @Test
    @Order(32)
    void createBlockRequest_aliceForCard2() {
        var req = new CardBlockRequestRequest(aliceCardIds.get(1), aliceId);
        HttpEntity<CardBlockRequestRequest> entity = new HttpEntity<>(req, authHeaders());
        ResponseEntity<CardBlockRequestResponse> resp = restTemplate.exchange("/api/admin/block-requests", HttpMethod.POST, entity, CardBlockRequestResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals("PENDING", resp.getBody().blockRequestStatus().name());
    }

    @Test
    @Order(33)
    void getPendingRequests_shouldReturn200() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/block-requests/pending?page=0&size=10", HttpMethod.GET, entity, Void.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(34)
    void approveBlockRequest_shouldBlockCard() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<String> listResp = restTemplate.exchange(
                "/api/block-requests/pending?page=0&size=10", HttpMethod.GET, h, String.class);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());

        try {
            JsonNode root = new ObjectMapper().readTree(listResp.getBody());
            JsonNode content = root.get("content");
            assertNotNull(content);
            assertTrue(content.size() > 0);

            Long requestId = content.get(0).get("id").asLong();

            ResponseEntity<CardBlockRequestAdminResponse> approveResp = restTemplate.exchange(
                    "/api/block-requests/" + requestId + "/approve", HttpMethod.PATCH, h, CardBlockRequestAdminResponse.class);
            assertEquals(HttpStatus.OK, approveResp.getStatusCode());
            assertEquals("APPROVED", approveResp.getBody().blockRequestStatus().name());

            ResponseEntity<CardAdminResponse> cardResp = restTemplate.exchange(
                    "/api/admin/cards/" + aliceCardIds.get(1), HttpMethod.GET, h, CardAdminResponse.class);
            assertEquals(CardStatus.BLOCKED, cardResp.getBody().cardStatus());

            restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(1) + "/activate", HttpMethod.PUT, h, Void.class);
        } catch (Exception e) {
            fail("Failed to parse pending requests: " + e.getMessage());
        }
    }

    @Test
    @Order(35)
    void approveAlreadyApproved_shouldReturn400() {
        var createReq = new CardBlockRequestRequest(aliceCardIds.get(4), aliceId);
        HttpEntity<CardBlockRequestRequest> createEntity = new HttpEntity<>(createReq, authHeaders());
        ResponseEntity<CardBlockRequestResponse> createResp = restTemplate.exchange(
                "/api/admin/block-requests", HttpMethod.POST, createEntity, CardBlockRequestResponse.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        // CardBlockRequestResponse doesn't have id, so query pending requests to find it
        HttpEntity<Void> h2 = new HttpEntity<>(authHeaders());
        ResponseEntity<String> pendingResp = restTemplate.exchange(
                "/api/block-requests/pending?page=0&size=10", HttpMethod.GET, h2, String.class);
        Long requestId;
        try {
            requestId = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(pendingResp.getBody()).get("content").get(0).get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        restTemplate.exchange("/api/block-requests/" + requestId + "/approve", HttpMethod.PATCH, h, Void.class);
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/block-requests/" + requestId + "/approve", HttpMethod.PATCH, h, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());

        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(4) + "/activate", HttpMethod.PUT, h, Void.class);
    }

    @Test
    @Order(36)
    void createAndRejectBlockRequest() {
        var req = new CardBlockRequestRequest(bobCardIds.get(0), bobId);
        HttpEntity<CardBlockRequestRequest> createEntity = new HttpEntity<>(req, authHeaders());
        ResponseEntity<CardBlockRequestResponse> createResp = restTemplate.exchange(
                "/api/admin/block-requests", HttpMethod.POST, createEntity, CardBlockRequestResponse.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        // CardBlockRequestResponse doesn't have id, so query pending requests to find it
        HttpEntity<Void> h2 = new HttpEntity<>(authHeaders());
        ResponseEntity<String> pendingResp = restTemplate.exchange(
                "/api/block-requests/pending?page=0&size=10", HttpMethod.GET, h2, String.class);
        Long requestId;
        try {
            requestId = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(pendingResp.getBody()).get("content").get(0).get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<CardBlockRequestAdminResponse> rejectResp = restTemplate.exchange(
                "/api/block-requests/" + requestId + "/reject", HttpMethod.PATCH, h, CardBlockRequestAdminResponse.class);
        assertEquals(HttpStatus.OK, rejectResp.getStatusCode());
        assertEquals("REJECTED", rejectResp.getBody().blockRequestStatus().name());

        ResponseEntity<CardAdminResponse> cardResp = restTemplate.exchange(
                "/api/admin/cards/" + bobCardIds.get(0), HttpMethod.GET, h, CardAdminResponse.class);
        assertEquals(CardStatus.ACTIVE, cardResp.getBody().cardStatus());
    }

    @Test
    @Order(37)
    void blockRequest_forNonActiveCard_shouldReturn400() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(0) + "/block", HttpMethod.PUT, h, Void.class);
        var req = new CardBlockRequestRequest(aliceCardIds.get(0), aliceId);
        HttpEntity<CardBlockRequestRequest> entity = new HttpEntity<>(req, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/block-requests", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        restTemplate.exchange("/api/admin/cards/" + aliceCardIds.get(0) + "/activate", HttpMethod.PUT, h, Void.class);
    }

    @Test
    @Order(38)
    void blockRequest_forOtherPersonCard_shouldReturn400() {
        var req = new CardBlockRequestRequest(bobCardIds.get(0), aliceId);
        HttpEntity<CardBlockRequestRequest> entity = new HttpEntity<>(req, authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/block-requests", HttpMethod.POST, entity, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ==================== PHASE 6: Card Deletion ====================

    @Test
    @Order(39)
    void deleteAliceCard_shouldReturn204() {
        Long cardToDelete = aliceCardIds.get(0);
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/cards/" + cardToDelete, HttpMethod.DELETE, h, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        aliceCardIds.remove(0);

        ResponseEntity<CardAdminResponse> getResp = restTemplate.exchange("/api/admin/cards/" + cardToDelete, HttpMethod.GET, h, CardAdminResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, getResp.getStatusCode());
    }

    @Test
    @Order(40)
    void deleteNonExistentCard_shouldReturn404() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/cards/99999", HttpMethod.DELETE, h, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ==================== PHASE 7: Person Deletion ====================

    @Test
    @Order(41)
    void deletePersonWithCards_shouldReturn400() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + aliceId, HttpMethod.DELETE, h, Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @Order(42)
    void deleteAllRemainingAliceCards() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        for (Long cardId : new ArrayList<>(aliceCardIds)) {
            ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/cards/" + cardId, HttpMethod.DELETE, h, Void.class);
            assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        }
        aliceCardIds.clear();
    }

    @Test
    @Order(43)
    void deleteAlice_shouldReturn204() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + aliceId, HttpMethod.DELETE, h, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
    }

    @Test
    @Order(44)
    void deleteNonExistentPerson_shouldReturn404() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/99999", HttpMethod.DELETE, h, Void.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    @Order(45)
    void deleteAllBobCardsAndBob() {
        HttpEntity<Void> h = new HttpEntity<>(authHeaders());
        for (Long cardId : bobCardIds) {
            restTemplate.exchange("/api/admin/cards/" + cardId, HttpMethod.DELETE, h, Void.class);
        }
        bobCardIds.clear();
        ResponseEntity<Void> resp = restTemplate.exchange("/api/persons/" + bobId, HttpMethod.DELETE, h, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
    }
}
