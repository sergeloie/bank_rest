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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock private PersonRepository personRepository;
    @Mock private CardRepository cardRepository;
    @Mock private PersonMapper personMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PersonService personService;

    @Test
    void getAll_shouldReturnPage() {
        Person person = createPerson(1L, "Alice");
        PersonAdminResponse dto = createAdminResponse("Alice");
        Page<Person> page = new PageImpl<>(List.of(person));
        when(personRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(personMapper.toAdminResponse(person)).thenReturn(dto);

        Page<PersonAdminResponse> result = personService.getAll(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("Alice", result.getContent().get(0).name());
    }

    @Test
    void getById_shouldReturnPerson() {
        Person person = createPerson(1L, "Alice");
        PersonAdminResponse dto = createAdminResponse("Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personMapper.toAdminResponse(person)).thenReturn(dto);

        PersonAdminResponse result = personService.getById(1L);

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
        PersonAdminResponse dto = createAdminResponse("Alice");
        when(personMapper.toEntity(request)).thenReturn(person);
        when(personRepository.save(person)).thenReturn(person);
        when(personMapper.toAdminResponse(person)).thenReturn(dto);

        PersonAdminResponse result = personService.create(request);

        assertEquals("Alice", result.name());
        verify(personRepository).save(person);
    }

    @Test
    void create_shouldThrowOnDuplicateName() {
        PersonCreateRequest request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        Person person = createPerson(1L, "Alice");
        when(personMapper.toEntity(request)).thenReturn(person);
        when(personRepository.save(person)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateResourceException.class, () -> personService.create(request));
    }

    @Test
    void update_shouldUpdatePasswordAndRole() {
        Person person = createPerson(1L, "Alice");
        PersonUpdateRequest request = new PersonUpdateRequest("newpass", Role.ADMIN);
        PersonAdminResponse dto = createAdminResponse("Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenReturn(person);
        when(personMapper.toAdminResponse(any(Person.class))).thenReturn(dto);

        PersonAdminResponse result = personService.update(1L, request);

        assertNotNull(result);
        verify(personMapper).updateEntity(request, person);
        verify(personRepository).save(person);
    }

    @Test
    void update_shouldIncrementPasswordVersionWhenPasswordChanges() {
        Person person = createPerson(1L, "Alice");
        person.setPasswordVersion(0L);
        PersonUpdateRequest request = new PersonUpdateRequest("newpass", null);
        PersonAdminResponse dto = createAdminResponse("Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenReturn(person);
        when(personMapper.toAdminResponse(any(Person.class))).thenReturn(dto);

        personService.update(1L, request);

        assertEquals(1L, person.getPasswordVersion());
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        PersonUpdateRequest request = new PersonUpdateRequest("pass", Role.USER);
        when(personRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> personService.update(99L, request));
    }

    @Test
    void delete_shouldDeletePerson() {
        Person person = createPerson(1L, "Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(cardRepository.existsByPerson_Id(1L)).thenReturn(false);

        personService.delete(1L);

        verify(personRepository).delete(person);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> personService.delete(99L));
    }

    @Test
    void delete_shouldThrowWhenPersonHasCards() {
        Person person = createPerson(1L, "Alice");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(cardRepository.existsByPerson_Id(1L)).thenReturn(true);

        assertThrows(InvalidCardOperationException.class, () -> personService.delete(1L));
        verify(personRepository, never()).delete(any());
    }

    private Person createPerson(Long id, String name) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPassword("pass");
        p.setRole(Role.USER);
        return p;
    }

    private PersonAdminResponse createAdminResponse(String name) {
        return new PersonAdminResponse(1L, name, Role.USER, null, null, null, null);
    }
}
