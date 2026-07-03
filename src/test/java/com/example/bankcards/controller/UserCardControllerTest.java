package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.CardSecurity;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private CardBlockRequestService cardBlockRequestService;

    @MockitoBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @MockitoBean(name = "cardSecurity")
    private CardSecurity cardSecurity;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getMyCards_shouldReturnPage() throws Exception {
        UUID personId = UUID.randomUUID();
        CardResponse dto = new CardResponse(UUID.randomUUID(), "**** **** **** 7890", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100));
        when(cardService.getCardsByPersonUser(eq(personId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/cards/person/" + personId).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 7890"));
    }

    @Test
    void getMyCards_shouldReturnEmptyPageWhenNoCards() throws Exception {
        UUID personId = UUID.randomUUID();
        when(cardService.getCardsByPersonUser(eq(personId), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/cards/person/" + personId).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getMyCardById_shouldReturnCard() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardResponse dto = new CardResponse(cardId, "**** **** **** 7890", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100));
        when(cardService.getCardByIdUser(cardId)).thenReturn(dto);

        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 7890"));
    }

    @Test
    void getMyCardById_shouldReturn404() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCardByIdUser(cardId)).thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/" + cardId))
                .andExpect(status().isNotFound());
    }
}
