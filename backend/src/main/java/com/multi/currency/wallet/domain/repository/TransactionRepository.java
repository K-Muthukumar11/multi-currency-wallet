package com.multi.currency.wallet.domain.repository;

import java.util.UUID;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.multi.currency.wallet.domain.model.Transaction;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> saveAll(List<Transaction> transactions);

    Optional<Transaction> findById(UUID id);

    Page<Transaction> findByAccountId(UUID accountId, Pageable pageable);

    List<Transaction> findByAccountId(UUID accountId);

    boolean existsByReferenceTransactionId(UUID referenceTransactionId);
}
