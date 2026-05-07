package com.multi.currency.wallet.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.multi.currency.wallet.infrastructure.persistence.entity.AccountJpaEntity;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {
    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);

    List<AccountJpaEntity> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);

}
