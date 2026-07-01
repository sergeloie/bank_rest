package com.example.bankcards.integration;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.PersonRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    private Long aliceId;
    private Long bobId;
    private Long bobCardId;

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

        // Create Bob
        var bobReq = new PersonCreateRequest("Bob", "pass456", Role.USER);
        restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(bobReq, authHeaders(adminToken)), Void.class);
        var bobLogin = restTemplate.postForEntity("/api/auth/login", new AuthRequest("Bob", "pass456"), AuthResponse.class);
        bobToken = bobLogin.getBody().accessToken();
        bobId = jdbcTemplate.queryForObject("SELECT id FROM person WHERE name = 'Bob'", Long.class);

        // Create Bob's card
        var cardReq = new CardCreateRequest(bobId, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        var cardResp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, new HttpEntity<>(cardReq, authHeaders(adminToken)), Object.class);
        // We know the ID will be 1 or something small, let's just query it
        bobCardId = jdbcTemplate.queryForObject("SELECT id FROM card WHERE person_id = ?", Long.class, bobId);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void testH2ConsoleAccessible() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/h2-console/", String.class);
        // Expecting it to be accessible (should fail if we want to fix it)
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testAliceBlockBobCard_shouldFailPrivilegeCheck() {
        // Alice should NOT be able to request block for Bob's card
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(aliceToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/cards/" + bobCardId + "/block-request", HttpMethod.PUT, entity, Void.class);
        
        System.out.println("Block card status: " + resp.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(), "Should be forbidden for Alice to block Bob's card");
    }

    @Test
    void testAliceCallAdminEndpoint_shouldBeForbidden() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(aliceToken));
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/admin/users", HttpMethod.GET, entity, Void.class);
        
        System.out.println("Admin users status: " + resp.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(), "Should be forbidden for Alice to access admin endpoint");
    }
}
