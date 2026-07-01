package com.example.bankcards.security;

import com.example.bankcards.entity.Person;
import com.example.bankcards.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PersonRepository personRepository;

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        Person person = personRepository.findByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + name));

        return new User(
                person.getName(),
                person.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + person.getRole().name()))
        );
    }
}
