package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardAdminResponse;
import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.CardSecurity;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.CardService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @MockitoBean(name = "cardSecurity")
    private CardSecurity cardSecurity;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getAllCards_shouldReturnPage() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        CardAdminResponse dto = new CardAdminResponse(cardId, personId, "Alice", "**** **** **** 7890",
                LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100),
                Instant.now(), Instant.now(), null, null);
        when(cardService.getAllCardsAdmin(eq(personId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/cards")
                        .param("personId", personId.toString())
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].maskedNumber").value("**** **** **** 7890"));
    }

    @Test
    void getCardById_shouldReturnCard() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        CardAdminResponse dto = new CardAdminResponse(cardId, personId, "Alice", "**** **** **** 7890",
                LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100),
                Instant.now(), Instant.now(), null, null);
        when(cardService.getCardByIdAdmin(cardId)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/cards/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 7890"));
    }

    @Test
    void getCardById_shouldReturn404() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCardByIdAdmin(cardId)).thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/admin/cards/" + cardId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_shouldReturn201() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        CardCreateRequest request = new CardCreateRequest(personId, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        CardAdminResponse dto = new CardAdminResponse(cardId, personId, "Alice", "**** **** **** 7890",
                LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100),
                Instant.now(), Instant.now(), null, null);
        when(cardService.createCard(any(CardCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"));
    }

    @Test
    void blockCard_shouldReturn200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        CardAdminResponse dto = new CardAdminResponse(cardId, personId, "Alice", "**** **** **** 7890",
                LocalDate.now().plusYears(1), CardStatus.BLOCKED, BigDecimal.valueOf(100),
                Instant.now(), Instant.now(), null, null);
        when(cardService.blockCard(cardId)).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/cards/" + cardId + "/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("BLOCKED"));
    }

    @Test
    void blockCard_shouldReturn400WhenAlreadyBlocked() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.blockCard(cardId)).thenThrow(new InvalidCardOperationException("Only active cards can be blocked"));

        mockMvc.perform(patch("/api/admin/cards/" + cardId + "/block"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateCard_shouldReturn200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        CardAdminResponse dto = new CardAdminResponse(cardId, personId, "Alice", "**** **** **** 7890",
                LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100),
                Instant.now(), Instant.now(), null, null);
        when(cardService.activateCard(cardId)).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/cards/" + cardId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"));
    }

    @Test
    void activateCard_shouldReturn400WhenNotBlocked() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.activateCard(cardId)).thenThrow(new InvalidCardOperationException("Only blocked cards can be activated"));

        mockMvc.perform(patch("/api/admin/cards/" + cardId + "/activate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCard_shouldReturn204() throws Exception {
        UUID cardId = UUID.randomUUID();
        doNothing().when(cardService).deleteCard(cardId);

        mockMvc.perform(delete("/api/admin/cards/" + cardId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCard_shouldReturn404() throws Exception {
        UUID cardId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Card not found"))
                .when(cardService).deleteCard(cardId);

        mockMvc.perform(delete("/api/admin/cards/" + cardId))
                .andExpect(status().isNotFound());
    }
}
