package com.example.bankcards.service;

import com.example.bankcards.dto.person.PersonAdminResponse;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonResponse;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.PersonMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PersonService {
    private final PersonRepository personRepository;
    private final CardRepository cardRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private static final String PERSON_NOT_FOUND = "Person not found with id: %d";
    private static final String PERSON_EXISTS = "Person already exists with name: %s";

    @Transactional(readOnly = true)
    public Page<PersonAdminResponse> getAll(Pageable pageable) {
        return personRepository.findAll(pageable).map(personMapper::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public PersonAdminResponse getById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        return personMapper.toAdminResponse(person);
    }

    @Transactional
    public PersonAdminResponse create(PersonCreateRequest request) {
        Person person = personMapper.toEntity(request);
        person.setPassword(passwordEncoder.encode(request.password()));
        try {
            return personMapper.toAdminResponse(personRepository.save(person));
        } catch (DataIntegrityViolationException _) {
            throw new DuplicateResourceException(String.format(PERSON_EXISTS, request.name()));
        }
    }

    @Transactional
    public PersonAdminResponse update(Long id, PersonUpdateRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        personMapper.updateEntity(request, person);
        if (request.password() != null) {
            person.setPassword(passwordEncoder.encode(request.password()));
            person.setPasswordVersion(person.getPasswordVersion() + 1);
        }
        return personMapper.toAdminResponse(personRepository.save(person));
    }

    @Transactional
    public void delete(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        if (cardRepository.existsByPerson_Id(id)) {
            throw new InvalidCardOperationException("Cannot delete person with existing cards");
        }
        personRepository.delete(person);
    }
}
