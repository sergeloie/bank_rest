package com.example.bankcards.integration;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.person.PersonCreateRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewFullIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private UUID userId;

    @BeforeAll
    void setup() {
        jdbcTemplate.update("UPDATE person SET password = ? WHERE name = 'admin'", passwordEncoder.encode("testadmin"));

        adminToken = login("admin", "testadmin");

        var userReq = new PersonCreateRequest("User", "userpass1");
        restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(userReq, adminHeaders()), Void.class);
        userId = jdbcTemplate.queryForObject("SELECT id FROM person WHERE name = 'User'", UUID.class);
        userToken = login("User", "userpass1");
    }

    private String login(String name, String password) {
        var resp = restTemplate.postForEntity("/api/auth/login", new AuthRequest(name, password), AuthResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return resp.getBody().accessToken();
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(adminToken);
        return h;
    }

    private HttpHeaders userHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(userToken);
        return h;
    }

    @Test
    @Order(1)
    void adminCanCreateCard() {
        var req = new CardCreateRequest(userId, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        var resp = restTemplate.exchange("/api/admin/cards", HttpMethod.POST, new HttpEntity<>(req, adminHeaders()), CardAdminResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test
    @Order(2)
    void userCanViewOwnCards() {
        var resp = restTemplate.exchange("/api/cards/person/" + userId + "?page=0&size=10", HttpMethod.GET, new HttpEntity<>(userHeaders()), Object.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @Order(3)
    void adminCanCreateAndDeleteUser() {
        var userReq = new PersonCreateRequest("NewUser", "pass1234");
        var createResp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(userReq, adminHeaders()), Void.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());

        UUID newUserId = jdbcTemplate.queryForObject("SELECT id FROM person WHERE name = 'NewUser'", UUID.class);
        var deleteResp = restTemplate.exchange("/api/admin/users/" + newUserId, HttpMethod.DELETE, new HttpEntity<>(adminHeaders()), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());
    }

    @Test
    @Order(4)
    void userCannotCreateUser() {
        var userReq = new PersonCreateRequest("Hacker", "pass1234");
        var resp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, new HttpEntity<>(userReq, userHeaders()), Void.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }
}
