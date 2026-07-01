package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    @EntityGraph(attributePaths = {"person"})
    Page<Card> findByPerson_Id(Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"person"})
    Page<Card> findByPerson_IdAndCardStatus(Long id, CardStatus status, Pageable pageable);

    List<Card> findByCardStatusAndExpirationDateBefore(CardStatus status, java.time.LocalDate date);

    boolean existsByCardHash(String cardHash);

    boolean existsByPerson_Id(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.id = :id")
    Optional<Card> findByIdForUpdate(Long id);

    @EntityGraph(attributePaths = {"person"})
    Page<Card> findByCardStatus(CardStatus status, Pageable pageable);
}
