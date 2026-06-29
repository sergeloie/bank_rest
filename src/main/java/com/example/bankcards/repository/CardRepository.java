package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findByPerson_Id(Long id, Pageable pageable);

    boolean existsByEncryptedNumber(String encryptedNumber);

    boolean existsByPerson_Id(Long id);


}