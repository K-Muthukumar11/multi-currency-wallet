package com.multi.currency.wallet.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.multi.currency.wallet.infrastructure.persistence.entity.TransactionJpaEntity;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    Page<TransactionJpaEntity> findByAccountId(UUID accountId, Pageable pageable);

    List<TransactionJpaEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    boolean existsByReferenceTransactionId(UUID referenceTransactionId);

}
