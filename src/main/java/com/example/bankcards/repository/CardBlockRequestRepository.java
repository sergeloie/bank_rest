package com.example.bankcards.repository;

import com.example.bankcards.entity.BlockRequestStatus;
import com.example.bankcards.entity.CardBlockRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBlockRequestRepository extends JpaRepository<CardBlockRequest, Long> {
    @EntityGraph(attributePaths = {"card", "person"})
    Page<CardBlockRequest> findByBlockRequestStatus(BlockRequestStatus blockRequestStatus, Pageable pageable);
}
