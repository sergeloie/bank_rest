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
        TransferHistoryResponse dto = new TransferHistoryResponse(
                1L, 1L, 1L, 2L, BigDecimal.valueOf(200), Instant.now());
        when(transferHistoryService.getTransfers(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/transfers")
                        .param("personId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].personId").value(1))
                .andExpect(jsonPath("$.content[0].fromCardId").value(1))
                .andExpect(jsonPath("$.content[0].toCardId").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(200));
    }

    @Test
    void getAllTransfers_shouldReturnAllTransfersWhenNoPersonId() throws Exception {
        TransferHistoryResponse dto1 = new TransferHistoryResponse(
                1L, 1L, 1L, 2L, BigDecimal.valueOf(200), Instant.now());
        TransferHistoryResponse dto2 = new TransferHistoryResponse(
                2L, 2L, 3L, 4L, BigDecimal.valueOf(100), Instant.now());
        when(transferHistoryService.getTransfers(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto1, dto2), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/admin/transfers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].personId").value(1))
                .andExpect(jsonPath("$.content[1].personId").value(2));
    }

    @Test
    void getAllTransfers_shouldReturnEmptyPageWhenNoTransfers() throws Exception {
        when(transferHistoryService.getTransfers(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/admin/transfers")
                        .param("personId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
