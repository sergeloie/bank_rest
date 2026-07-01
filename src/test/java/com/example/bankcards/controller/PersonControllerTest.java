package com.example.bankcards.controller;

import com.example.bankcards.dto.person.PersonAdminResponse;
import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.DuplicateResourceException;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.PersonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PersonService personService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @Test
    void getAllUsers_shouldReturnPage() throws Exception {
        PersonAdminResponse dto = new PersonAdminResponse(1L, "Alice", Role.USER, null, null, null, null);
        when(personService.getAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/users").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Alice"));
    }

    @Test
    void getUserById_shouldReturnPerson() throws Exception {
        PersonAdminResponse dto = new PersonAdminResponse(1L, "Alice", Role.USER, null, null, null, null);
        when(personService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getUserById_shouldReturn404() throws Exception {
        when(personService.getById(99L)).thenThrow(new ResourceNotFoundException("Person not found"));

        mockMvc.perform(get("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_shouldReturn201() throws Exception {
        PersonCreateRequest request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        PersonAdminResponse dto = new PersonAdminResponse(1L, "Alice", Role.USER, null, null, null, null);
        when(personService.create(any(PersonCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void createUser_shouldReturn409OnDuplicate() throws Exception {
        PersonCreateRequest request = new PersonCreateRequest("Alice", "pass123", Role.USER);
        when(personService.create(any(PersonCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Person already exists"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_shouldReturn400OnValidation() throws Exception {
        PersonCreateRequest request = new PersonCreateRequest("", "", null);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        doNothing().when(personService).delete(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_shouldReturn404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Person not found"))
                .when(personService).delete(99L);

        mockMvc.perform(delete("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_shouldReturn400WhenPersonHasCards() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidCardOperationException("Cannot delete person with existing cards"))
                .when(personService).delete(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isBadRequest());
    }
}
