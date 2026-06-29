package com.example.bankcards.service;

import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonDto;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.PersonMapper;
import com.example.bankcards.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PersonService {
    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private static final String PERSON_NOT_FOUND = "Person not found with id: %d";
    private static final String PERSON_EXISTS = "Person already exists with name: %s";

    public Page<PersonDto> getAll(Pageable pageable) {
        return personRepository.findAll(pageable).map(personMapper::toPersonDto);
    }

    public PersonDto getById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        return personMapper.toPersonDto(person);
    }

    public PersonDto create(PersonCreateRequest request) {
        if (personRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(String.format(PERSON_EXISTS, request.name()));
        }
        Person person = personMapper.toEntity(request);
        return personMapper.toPersonDto(personRepository.save(person));
    }

    public PersonDto update(Long id, PersonUpdateRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));

        if (!person.getName().equals(request.name()) && personRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(String.format(PERSON_EXISTS, request.name()));
        }

        person.setName(request.name());
        person.setPassword(request.password());
        person.setRole(request.role());
        return personMapper.toPersonDto(personRepository.save(person));
    }

    public void delete(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PERSON_NOT_FOUND, id)));
        personRepository.delete(person);
    }
}
