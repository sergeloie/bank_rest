package com.example.bankcards.security;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBKV1QgdG9rZW4gc2lnbmluZyE=");
        props.setAccessExpiration(1800000);
        props.setRefreshExpiration(604800000);
        jwtTokenProvider = new JwtTokenProvider(props);
    }

    @Test
    void generateAccessToken_shouldReturnValidToken() {
        Person person = createPerson();

        String token = jwtTokenProvider.generateAccessToken(person);

        assertNotNull(token);
        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals(person.getId().toString(), claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
        assertEquals(0L, claims.get("passwordVersion", Long.class));
    }

    @Test
    void generateRefreshToken_shouldReturnValidToken() {
        Person person = createPerson();

        String token = jwtTokenProvider.generateRefreshToken(person);

        assertNotNull(token);
        Claims claims = jwtTokenProvider.validateToken(token);
        assertEquals(person.getId().toString(), claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
        assertEquals(0L, claims.get("passwordVersion", Long.class));
    }

    @Test
    void validateToken_shouldThrowOnInvalidToken() {
        assertThrows(JwtException.class, () -> jwtTokenProvider.validateToken("invalid-token"));
    }

    @Test
    void getPersonId_shouldExtractId() {
        Person person = createPerson();
        String token = jwtTokenProvider.generateAccessToken(person);

        UUID personId = jwtTokenProvider.getPersonId(token);

        assertEquals(person.getId(), personId);
    }

    @Test
    void getRole_shouldExtractRole() {
        String token = jwtTokenProvider.generateAccessToken(createPerson());

        Role role = jwtTokenProvider.getRole(token);

        assertEquals(Role.ADMIN, role);
    }

    @Test
    void isRefreshToken_shouldReturnTrueForRefresh() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(createPerson());
        String accessToken = jwtTokenProvider.generateAccessToken(createPerson());

        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
    }

    @Test
    void getPasswordVersion_shouldExtractVersion() {
        Person person = createPerson();
        person.setPasswordVersion(5L);
        String token = jwtTokenProvider.generateAccessToken(person);
        Claims claims = jwtTokenProvider.validateToken(token);

        Long passwordVersion = jwtTokenProvider.getPasswordVersion(claims);

        assertEquals(5L, passwordVersion);
    }

    private Person createPerson() {
        Person p = new Person();
        p.setId(UUID.randomUUID());
        p.setName("admin");
        p.setPassword("pass");
        p.setRole(Role.ADMIN);
        return p;
    }
}
