package com.multi.currency.wallet.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.Money;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.infrastructure.persistence.entity.AccountJpaEntity;
import com.multi.currency.wallet.infrastructure.persistence.jpa.AccountJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = toEntity(account);
        AccountJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public List<Account> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpaRepository.existsByAccountNumber(accountNumber);
    }

    private AccountJpaEntity toEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getId());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setUserId(account.getUserId());
        entity.setBalance(account.getBalance().getAmount());
        entity.setCurrencyCode(account.getCurrencyCode());
        entity.setStatus(account.getStatus());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());
        return entity;
    }

    private Account toDomain(AccountJpaEntity entity) {
        Money balance = Money.of(entity.getBalance(), entity.getCurrencyCode());
        return new Account(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getUserId(),
                balance,
                entity.getCurrencyCode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

}
