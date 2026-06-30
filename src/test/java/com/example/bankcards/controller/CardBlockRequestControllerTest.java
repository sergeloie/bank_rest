package com.example.bankcards.controller;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.exception.InvalidCardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.CardBlockRequestService;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardBlockRequestController.class)
class CardBlockRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardBlockRequestService cardBlockRequestService;

    @MockitoBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @Test
    void createBlockRequest_shouldReturn201() throws Exception {
        CardBlockRequestRequest request = new CardBlockRequestRequest(1L, 1L);
        CardBlockRequestResponse dto = new CardBlockRequestResponse(1L, 1L, 1L, BlockRequestStatus.PENDING);
        when(cardBlockRequestService.createRequest(any(CardBlockRequestRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/block-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockRequestStatus").value("PENDING"));
    }

    @Test
    void createBlockRequest_shouldReturn400OnValidation() throws Exception {
        CardBlockRequestRequest request = new CardBlockRequestRequest(null, null);

        mockMvc.perform(post("/api/block-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPendingRequests_shouldReturnPage() throws Exception {
        CardBlockRequestResponse dto = new CardBlockRequestResponse(1L, 1L, 1L, BlockRequestStatus.PENDING);
        when(cardBlockRequestService.getPendingRequests(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/block-requests/pending").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].blockRequestStatus").value("PENDING"));
    }

    @Test
    void approveRequest_shouldReturn200() throws Exception {
        CardBlockRequestResponse dto = new CardBlockRequestResponse(1L, 1L, 1L, BlockRequestStatus.APPROVED);
        when(cardBlockRequestService.approveRequest(1L)).thenReturn(dto);

        mockMvc.perform(patch("/api/block-requests/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockRequestStatus").value("APPROVED"));
    }

    @Test
    void approveRequest_shouldReturn400WhenNotPending() throws Exception {
        when(cardBlockRequestService.approveRequest(1L))
                .thenThrow(new InvalidCardOperationException("Only pending requests can be approved"));

        mockMvc.perform(patch("/api/block-requests/1/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveRequest_shouldReturn404() throws Exception {
        when(cardBlockRequestService.approveRequest(99L))
                .thenThrow(new ResourceNotFoundException("Block request not found"));

        mockMvc.perform(patch("/api/block-requests/99/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectRequest_shouldReturn200() throws Exception {
        CardBlockRequestResponse dto = new CardBlockRequestResponse(1L, 1L, 1L, BlockRequestStatus.REJECTED);
        when(cardBlockRequestService.rejectRequest(1L)).thenReturn(dto);

        mockMvc.perform(patch("/api/block-requests/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockRequestStatus").value("REJECTED"));
    }

    @Test
    void rejectRequest_shouldReturn400WhenNotPending() throws Exception {
        when(cardBlockRequestService.rejectRequest(1L))
                .thenThrow(new InvalidCardOperationException("Only pending requests can be rejected"));

        mockMvc.perform(patch("/api/block-requests/1/reject"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectRequest_shouldReturn404() throws Exception {
        when(cardBlockRequestService.rejectRequest(99L))
                .thenThrow(new ResourceNotFoundException("Block request not found"));

        mockMvc.perform(patch("/api/block-requests/99/reject"))
                .andExpect(status().isNotFound());
    }
}
