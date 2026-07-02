package com.example.bankcards.repository;

import com.example.bankcards.entity.TransferHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {
    @EntityGraph(attributePaths = {"person", "fromCard", "toCard"})
    Page<TransferHistory> findByPerson_Id(UUID personId, Pageable pageable);
}
