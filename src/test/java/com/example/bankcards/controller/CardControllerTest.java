package com.example.bankcards.controller;

import com.example.bankcards.dto.card.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @Test
    void getCardsByPerson_shouldReturnPage() throws Exception {
        CardResponse dto = new CardResponse(1L, 1L, "Alice", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100), null, null);
        when(cardService.getCardsByPerson(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/cards/person/1").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].cardStatus").value("ACTIVE"));
    }

    @Test
    void getCardById_shouldReturnCard() throws Exception {
        CardResponse dto = new CardResponse(1L, 1L, "Alice", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100), null, null);
        when(cardService.getCardById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getCardById_shouldReturn404() throws Exception {
        when(cardService.getCardById(99L)).thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_shouldReturn201() throws Exception {
        CardCreateRequest request = new CardCreateRequest(1L, LocalDate.now().plusYears(1), BigDecimal.valueOf(100));
        CardResponse dto = new CardResponse(1L, 1L, "Alice", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100), null, null);
        when(cardService.createCard(any(CardCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createCard_shouldReturn400OnValidation() throws Exception {
        CardCreateRequest request = new CardCreateRequest(null, null, null);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockCard_shouldReturn200() throws Exception {
        CardResponse dto = new CardResponse(1L, 1L, "Alice", LocalDate.now().plusYears(1), CardStatus.BLOCKED, BigDecimal.valueOf(100), null, null);
        when(cardService.blockCard(1L)).thenReturn(dto);

        mockMvc.perform(patch("/api/cards/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("BLOCKED"));
    }

    @Test
    void blockCard_shouldReturn400WhenAlreadyBlocked() throws Exception {
        when(cardService.blockCard(1L)).thenThrow(new InvalidCardOperationException("Only active cards can be blocked"));

        mockMvc.perform(patch("/api/cards/1/block"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateCard_shouldReturn200() throws Exception {
        CardResponse dto = new CardResponse(1L, 1L, "Alice", LocalDate.now().plusYears(1), CardStatus.ACTIVE, BigDecimal.valueOf(100), null, null);
        when(cardService.activateCard(1L)).thenReturn(dto);

        mockMvc.perform(patch("/api/cards/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"));
    }

    @Test
    void deleteCard_shouldReturn204() throws Exception {
        doNothing().when(cardService).deleteCard(1L);

        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCard_shouldReturn404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Card not found"))
                .when(cardService).deleteCard(99L);

        mockMvc.perform(delete("/api/cards/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_shouldReturn200() throws Exception {
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(100), 1L);
        CardTransferResponse dto = new CardTransferResponse(1L, 2L, BigDecimal.valueOf(100), BigDecimal.valueOf(400), BigDecimal.valueOf(200));
        when(transferService.transfer(any(CardTransferRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void transfer_shouldReturn400OnInsufficientFunds() throws Exception {
        CardTransferRequest request = new CardTransferRequest(1L, 2L, BigDecimal.valueOf(1000), 1L);
        when(transferService.transfer(any(CardTransferRequest.class)))
                .thenThrow(new InvalidCardOperationException("Insufficient funds"));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
