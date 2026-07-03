package com.example.bankcards.integration;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExceptionHandlingAuditTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeAll
    void setup() {
        String hashed = passwordEncoder.encode("testadmin");
        jdbcTemplate.update("UPDATE person SET password = ? WHERE name = 'admin'", hashed);

        var loginReq = new AuthRequest("admin", "testadmin");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, AuthResponse.class);
        adminToken = loginResp.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void testCreatePersonWithInvalidData_shouldReturn400() {
        var req = new PersonCreateRequest("", "");
        HttpEntity<PersonCreateRequest> entity = new HttpEntity<>(req, authHeaders(adminToken));
        ResponseEntity<Void> resp = restTemplate.exchange("/api/admin/users", HttpMethod.POST, entity, Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
