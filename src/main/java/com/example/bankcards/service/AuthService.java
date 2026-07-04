package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.RefreshRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.exception.AuthenticationFailedException;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        Person person = personRepository.findByName(request.name())
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for unknown user: {}", request.name());
                    return new AuthenticationFailedException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.password(), person.getPassword())) {
            log.warn("Failed login attempt for user: {} (wrong password)", request.name());
            throw new AuthenticationFailedException("Invalid credentials");
        }

        log.info("User logged in: {}", request.name());
        String accessToken = jwtTokenProvider.generateAccessToken(person);
        String refreshToken = jwtTokenProvider.generateRefreshToken(person);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        try {
            jwtTokenProvider.validateToken(request.refreshToken());
            if (!jwtTokenProvider.isRefreshToken(request.refreshToken())) {
                throw new AuthenticationFailedException("Not a refresh token");
            }

            UUID personId = jwtTokenProvider.getPersonId(request.refreshToken());
            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> new AuthenticationFailedException("User not found"));

            log.info("Token refreshed for user: {}", person.getName());
            String accessToken = jwtTokenProvider.generateAccessToken(person);
            String refreshToken = jwtTokenProvider.generateRefreshToken(person);

            return new AuthResponse(accessToken, refreshToken);
        } catch (AuthenticationFailedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Invalid refresh token attempt: {}", e.getMessage());
            throw new AuthenticationFailedException("Invalid refresh token");
        }
    }
}
