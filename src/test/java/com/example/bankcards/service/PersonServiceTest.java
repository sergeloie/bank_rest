package com.example.bankcards.service;

import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonResponse;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.entity.Person;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.PersonMapper;
import com.example.bankcards.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private PersonService personService;

    @Test
    void getAll_shouldReturnPage() {
        Person person = createPerson(1L, "Alice");
        PersonResponse dto = createPersonResponse(1L, "Alice");
        Page<Person> page = new PageImpl<>(List.of(person));
        when(personRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(personMapper.toPersonResponse(person)).thenReturn(dto);

        Page<PersonResponse> result = personService.getAll(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("Alice", result.getContent().get(0).name());
    }

    @Test
    void getById_shouldReturnPerson() {
        Person person = createPerson(1L, "Alice");
        PersonResponse dto = createPersonResponse(1L, "Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personMapper.toPersonResponse(person)).thenReturn(dto);

        PersonResponse result = personService.getById(1L);

        assertEquals("Alice", result.name());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.getById(99L));
    }

    @Test
    void create_shouldCreatePerson() {
        PersonCreateRequest request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        Person person = createPerson(1L, "Alice");
        PersonResponse dto = createPersonResponse(1L, "Alice");
        when(personRepository.existsByName("Alice")).thenReturn(false);
        when(personMapper.toEntity(request)).thenReturn(person);
        when(personRepository.save(person)).thenReturn(person);
        when(personMapper.toPersonResponse(person)).thenReturn(dto);

        PersonResponse result = personService.create(request);

        assertEquals("Alice", result.name());
        verify(personRepository).save(person);
    }

    @Test
    void create_shouldThrowOnDuplicateName() {
        PersonCreateRequest request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        when(personRepository.existsByName("Alice")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> personService.create(request));
    }

    @Test
    void update_shouldUpdatePerson() {
        Person person = createPerson(1L, "Alice");
        PersonUpdateRequest request = new PersonUpdateRequest("Bob", "newpass", Role.ADMIN);
        PersonResponse dto = createPersonResponse(1L, "Bob");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personRepository.existsByName("Bob")).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenReturn(person);
        when(personMapper.toPersonResponse(any(Person.class))).thenReturn(dto);

        PersonResponse result = personService.update(1L, request);

        assertEquals("Bob", result.name());
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        PersonUpdateRequest request = new PersonUpdateRequest("Bob", "pass", Role.USER);
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.update(99L, request));
    }

    @Test
    void update_shouldThrowOnNameConflict() {
        Person person = createPerson(1L, "Alice");
        PersonUpdateRequest request = new PersonUpdateRequest("Bob", "pass", Role.USER);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personRepository.existsByName("Bob")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> personService.update(1L, request));
    }

    @Test
    void delete_shouldDeletePerson() {
        Person person = createPerson(1L, "Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        personService.delete(1L);

        verify(personRepository).delete(person);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> personService.delete(99L));
    }

    private Person createPerson(Long id, String name) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private PersonResponse createPersonResponse(Long id, String name) {
        return new PersonResponse(id, name, Role.USER, null, null);
    }
}
