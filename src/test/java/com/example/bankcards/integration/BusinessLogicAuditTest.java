package com.example.bankcards.integration;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardTransferRequest;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.entity.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BusinessLogicAuditTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String aliceToken;
    private Long aliceId;
    private Long card1Id;
    private Long card2Id;

    @BeforeAll
    void setup() {
        String hashed = passwordEncoder.encode("testadmin");
        jdbcTemplate.update("UPDATE person SET password = ? WHERE name = 'admin'", hashed);

        var loginReq = new AuthRequest("admin", "testadmin");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, AuthResponse.class);
        adminToken = loginResp.getBody().accessToken();

        // Create Alice
        var aliceReq = new PersonCreateRequest("Alice", "pass123", Role.USER);
        restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(aliceReq, authHeaders(adminToken)), Void.class);
        var aliceLogin = restTemplate.postForEntity("/api/auth/login", new AuthRequest("Alice", "pass123"), AuthResponse.class);
        aliceToken = aliceLogin.getBody().accessToken();
        aliceId = jdbcTemplate.queryForObject("SELECT id FROM person WHERE name = 'Alice'", Long.class);

        // Create Alice's cards
        var cardReq1 = new CardCreateRequest(aliceId, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        restTemplate.exchange("/api/admin/cards", HttpMethod.POST, new HttpEntity<>(cardReq1, authHeaders(adminToken)), Object.class);
        card1Id = jdbcTemplate.queryForObject("SELECT id FROM card WHERE person_id = ? AND balance = 100", Long.class, aliceId);

        var cardReq2 = new CardCreateRequest(aliceId, LocalDate.now().plusYears(1), BigDecimal.valueOf(50));
        restTemplate.exchange("/api/admin/cards", HttpMethod.POST, new HttpEntity<>(cardReq2, authHeaders(adminToken)), Object.class);
        card2Id = jdbcTemplate.queryForObject("SELECT id FROM card WHERE person_id = ? AND balance = 50", Long.class, aliceId);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void testTransferZeroAmount_shouldFail() {
        var req = new CardTransferRequest(card1Id, card2Id, BigDecimal.ZERO, aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, authHeaders(aliceToken));
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        
        // Should be 400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void testTransferNegativeAmount_shouldFail() {
        var req = new CardTransferRequest(card1Id, card2Id, BigDecimal.valueOf(-10), aliceId);
        HttpEntity<CardTransferRequest> entity = new HttpEntity<>(req, authHeaders(aliceToken));
        ResponseEntity<Void> resp = restTemplate.exchange("/api/transfers", HttpMethod.POST, entity, Void.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
