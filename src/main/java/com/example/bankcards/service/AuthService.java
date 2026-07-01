package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthRequest;
import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.RefreshRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.repository.PersonRepository;
import com.example.bankcards.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(AuthRequest request) {
        Person person = personRepository.findByName(request.name())
                .orElseThrow(() -> new InvalidCardOperationException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), person.getPassword())) {
            throw new InvalidCardOperationException("Invalid credentials");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(person);
        String refreshToken = jwtTokenProvider.generateRefreshToken(person);

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            jwtTokenProvider.validateToken(request.refreshToken());
            if (!jwtTokenProvider.isRefreshToken(request.refreshToken())) {
                throw new InvalidCardOperationException("Not a refresh token");
            }

            Long personId = jwtTokenProvider.getPersonId(request.refreshToken());
            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> new InvalidCardOperationException("User not found"));

            String accessToken = jwtTokenProvider.generateAccessToken(person);
            String refreshToken = jwtTokenProvider.generateRefreshToken(person);

            return new AuthResponse(accessToken, refreshToken);
        } catch (InvalidCardOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCardOperationException("Invalid refresh token");
        }
    }
}
