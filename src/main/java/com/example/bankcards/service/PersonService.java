package com.example.bankcards.service;

import com.example.bankcards.dto.person.PersonAdminResponse;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.PersonMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PersonService {
    private final PersonRepository personRepository;
    private final CardRepository cardRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private static final String PERSON_NOT_FOUND = "Person not found with id: %s";
    private static final String PERSON_EXISTS = "Person already exists with name: %s";

    @Transactional(readOnly = true)
    public Page<PersonAdminResponse> getAll(Pageable pageable) {
        return personRepository.findAll(pageable).map(personMapper::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public PersonAdminResponse getById(UUID id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        return personMapper.toAdminResponse(person);
    }

    @Transactional
    public PersonAdminResponse create(PersonCreateRequest request) {
        if (personRepository.findByName(request.name()).isPresent()) {
            throw new DuplicateResourceException(String.format(PERSON_EXISTS, request.name()));
        }
        Person person = personMapper.toEntity(request);
        person.setRole(Role.USER);
        person.setPassword(passwordEncoder.encode(request.password()));
        Person saved = personRepository.save(person);
        log.info("Person created: id={}, name={}", saved.getId(), saved.getName());
        return personMapper.toAdminResponse(saved);
    }

    @Transactional
    public PersonAdminResponse update(UUID id, PersonUpdateRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        personMapper.updateEntity(request, person);
        if (request.password() != null) {
            person.setPassword(passwordEncoder.encode(request.password()));
            person.setPasswordVersion(person.getPasswordVersion() + 1);
            log.info("Password changed for person: id={}", id);
        }
        PersonAdminResponse response = personMapper.toAdminResponse(personRepository.save(person));
        log.info("Person updated: id={}", id);
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        if (cardRepository.existsByPerson_Id(id)) {
            throw new InvalidCardOperationException("Cannot delete person with existing cards");
        }
        personRepository.delete(person);
        log.info("Person deleted: id={}", id);
    }
}
