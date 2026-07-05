package com.example.bankcards.integration;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.person.PersonCreateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityAuditTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String aliceToken;
    private String bobToken;
    private UUID aliceId;
    private UUID bobId;
    private UUID bobCardId;

    @BeforeAll
    void setup() {
        String hashed = passwordEncoder.encode("testadmin");
        jdbcTemplate.update("UPDATE person SET password = ? WHERE name = 'admin'", hashed);

        var loginReq = new AuthRequest("admin", "testadmin");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, AuthResponse.class);
        adminToken = loginResp.getBody().accessToken();

        var aliceReq = new PersonCreateRequest("Alice", "pass1234");
        restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(aliceReq, authHeaders(adminToken)), Void.class);
        var aliceLogin = restTemplate.postForEntity("/api/auth/login", new AuthRequest("Alice", "pass1234"), AuthResponse.class);
        aliceToken = aliceLogin.getBody().accessToken();
        aliceId = jdbcTemplate.queryForObject("SELECT id FROM person WHERE name = 'Alice'", UUID.class);

        var bobReq = new PersonCreateRequest("Bob", "pass4567");
        restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(bobReq, authHeaders(adminToken)), Void.class);
        var bobLogin = restTemplate.postForEntity("/api/auth/login", new AuthRequest("Bob", "pass4567"), AuthResponse.class);
        bobToken = bobLogin.getBody().accessToken();
        bobId = jdbcTemplate.queryForObject("SELECT id FROM person WHERE name = 'Bob'", UUID.class);

        var cardReq = new CardCreateRequest(bobId, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        var cardResp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, new HttpEntity<>(cardReq, authHeaders(adminToken)), Object.class);
        bobCardId = jdbcTemplate.queryForObject("SELECT id FROM card WHERE person_id = ?", UUID.class, bobId);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void testAliceBlockBobCard_shouldFailPrivilegeCheck() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(aliceToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/cards/" + bobCardId + "/block-request", HttpMethod.PUT, entity, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(), "Should be forbidden for Alice to block Bob's card");
    }

    @Test
    void testAliceCallAdminEndpoint_shouldBeForbidden() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(aliceToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/admin/users", HttpMethod.GET, entity, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(), "Should be forbidden for Alice to access admin endpoint");
    }
}
