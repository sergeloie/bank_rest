package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.RefreshRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnTokens() {
        Person person = createPerson();
        when(personRepository.findByName("admin")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(person)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(person)).thenReturn("refresh-token");

        AuthRequest request = new AuthRequest("admin", "secret");
        AuthResponse response = authService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void login_shouldThrowWhenUserNotFound() {
        when(personRepository.findByName("unknown")).thenReturn(Optional.empty());

        AuthRequest request = new AuthRequest("unknown", "pass");
        assertThrows(InvalidCardOperationException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowWhenPasswordMismatch() {
        Person person = createPerson();
        when(personRepository.findByName("admin")).thenReturn(Optional.of(person));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AuthRequest request = new AuthRequest("admin", "wrong");
        assertThrows(InvalidCardOperationException.class, () -> authService.login(request));
    }

    @Test
    void refresh_shouldReturnNewTokens() {
        Person person = createPerson();
        when(jwtTokenProvider.validateToken("refresh-token")).thenAnswer(i -> null);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getPersonId("refresh-token")).thenReturn(person.getId());
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(jwtTokenProvider.generateAccessToken(person)).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(person)).thenReturn("new-refresh");

        RefreshRequest request = new RefreshRequest("refresh-token");
        AuthResponse response = authService.refresh(request);

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void refresh_shouldThrowOnInvalidToken() {
        when(jwtTokenProvider.validateToken("bad-token")).thenThrow(new RuntimeException("invalid"));

        RefreshRequest request = new RefreshRequest("bad-token");
        assertThrows(InvalidCardOperationException.class, () -> authService.refresh(request));
    }

    private Person createPerson() {
        Person p = new Person();
        p.setId(UUID.randomUUID());
        p.setName("admin");
        p.setPassword("hashed");
        p.setRole(Role.ADMIN);
        return p;
    }
}
