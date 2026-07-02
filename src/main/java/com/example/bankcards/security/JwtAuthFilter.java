package com.example.bankcards.security;

import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.PersonRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final PersonRepository personRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.validateToken(token);
                String type = claims.get("type", String.class);

                if ("access".equals(type)) {
                    Long personId = Long.valueOf(claims.getSubject());
                    Person person = personRepository.findById(personId).orElse(null);

                    if (person != null) {
                        Long tokenPasswordVersion = jwtTokenProvider.getPasswordVersion(claims);
                        if (tokenPasswordVersion != null && tokenPasswordVersion.equals(person.getPasswordVersion())) {
                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + person.getRole().name());
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(person, null, List.of(authority));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        } else {
                            log.debug("Rejected stale JWT for person {} (passwordVersion mismatch)", personId);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
