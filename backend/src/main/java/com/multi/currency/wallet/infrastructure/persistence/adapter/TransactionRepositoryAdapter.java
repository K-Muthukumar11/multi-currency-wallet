package com.multi.currency.wallet.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.multi.currency.wallet.domain.model.Money;
import com.multi.currency.wallet.domain.model.Transaction;
import com.multi.currency.wallet.domain.repository.TransactionRepository;
import com.multi.currency.wallet.infrastructure.persistence.entity.TransactionJpaEntity;
import com.multi.currency.wallet.infrastructure.persistence.jpa.TransactionJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    @Override
    public Transaction save(Transaction transaction) {
        return toDomain(jpaRepository.save(toEntity(transaction)));
    }

    @Override
    public List<Transaction> saveAll(List<Transaction> transactions) {
        List<TransactionJpaEntity> entities = transactions.stream().map(this::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Transaction> findByAccountId(UUID accountId, Pageable pageable) {
        return jpaRepository.findByAccountId(accountId, pageable).map(this::toDomain);
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId) {
        return jpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByReferenceTransactionId(UUID referenceTransactionId) {
        return jpaRepository.existsByReferenceTransactionId(referenceTransactionId);
    }

    private TransactionJpaEntity toEntity(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setAccountId(transaction.getAccountId());
        entity.setType(transaction.getType());
        entity.setAmount(transaction.getAmount().getAmount());
        entity.setCurrencyCode(transaction.getAmount().getCurrencyCode());
        entity.setBalanceAfter(transaction.getBalanceAfter().getAmount());
        entity.setBalanceAfterCurrency(transaction.getBalanceAfter().getCurrencyCode());
        entity.setDescription(transaction.getDescription());
        entity.setReferenceTransactionId(transaction.getReferenceTransactionId());
        entity.setRelatedAccountId(transaction.getRelatedAccountId());
        entity.setStatus(transaction.getStatus());
        entity.setCreatedAt(transaction.getCreatedAt());
        return entity;
    }

    private Transaction toDomain(TransactionJpaEntity e) {
        return new Transaction.Builder()
                .id(e.getId())
                .accountId(e.getAccountId())
                .type(e.getType())
                .amount(Money.of(e.getAmount(), e.getCurrencyCode()))
                .balanceAfter(Money.of(e.getBalanceAfter(), e.getBalanceAfterCurrency()))
                .description(e.getDescription())
                .referenceTransactionId(e.getReferenceTransactionId())
                .relatedAccountId(e.getRelatedAccountId())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }

}
