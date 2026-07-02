package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferHistoryResponse;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.TransferHistoryService;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminTransferHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTransferHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferHistoryService transferHistoryService;

    @MockitoBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getAllTransfers_shouldReturnPage() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID fromCardId = UUID.randomUUID();
        UUID toCardId = UUID.randomUUID();
        TransferHistoryResponse dto = new TransferHistoryResponse(
                1L, personId, fromCardId, toCardId, BigDecimal.valueOf(200), Instant.now());
        when(transferHistoryService.getTransfers(eq(personId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/transfers")
                        .param("personId", personId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].personId").value(personId.toString()))
                .andExpect(jsonPath("$.content[0].fromCardId").value(fromCardId.toString()))
                .andExpect(jsonPath("$.content[0].toCardId").value(toCardId.toString()))
                .andExpect(jsonPath("$.content[0].amount").value(200));
    }

    @Test
    void getAllTransfers_shouldReturnAllTransfersWhenNoPersonId() throws Exception {
        UUID personId1 = UUID.randomUUID();
        UUID personId2 = UUID.randomUUID();
        UUID fromCardId1 = UUID.randomUUID();
        UUID toCardId1 = UUID.randomUUID();
        UUID fromCardId2 = UUID.randomUUID();
        UUID toCardId2 = UUID.randomUUID();
        TransferHistoryResponse dto1 = new TransferHistoryResponse(
                1L, personId1, fromCardId1, toCardId1, BigDecimal.valueOf(200), Instant.now());
        TransferHistoryResponse dto2 = new TransferHistoryResponse(
                2L, personId2, fromCardId2, toCardId2, BigDecimal.valueOf(100), Instant.now());
        when(transferHistoryService.getTransfers(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto1, dto2), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/admin/transfers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].personId").value(personId1.toString()))
                .andExpect(jsonPath("$.content[1].personId").value(personId2.toString()));
    }

    @Test
    void getAllTransfers_shouldReturnEmptyPageWhenNoTransfers() throws Exception {
        UUID personId = UUID.randomUUID();
        when(transferHistoryService.getTransfers(eq(personId), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/admin/transfers")
                        .param("personId", personId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
