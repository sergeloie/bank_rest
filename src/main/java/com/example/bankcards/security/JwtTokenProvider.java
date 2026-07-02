package com.example.bankcards.security;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Person person) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessExpiration());

        return Jwts.builder()
                .subject(String.valueOf(person.getId()))
                .claim("role", person.getRole().name())
                .claim("type", "access")
                .claim("passwordVersion", person.getPasswordVersion())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Person person) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .subject(String.valueOf(person.getId()))
                .claim("type", "refresh")
                .claim("passwordVersion", person.getPasswordVersion())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getPersonId(String token) {
        return Long.valueOf(validateToken(token).getSubject());
    }

    public Role getRole(String token) {
        return Role.valueOf(validateToken(token).get("role", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(validateToken(token).get("type", String.class));
    }

    public Long getPasswordVersion(Claims claims) {
        return claims.get("passwordVersion", Long.class);
    }
}
