package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
    @EntityGraph(attributePaths = {"person"})
    Page<Card> findByPerson_Id(Long id, Pageable pageable);

    boolean existsByEncryptedNumber(String encryptedNumber);

    boolean existsByPerson_Id(Long id);
}
