package com.multi.currency.wallet.domain.repository;

import java.util.UUID;
import java.util.Optional;
import java.util.List;
import com.multi.currency.wallet.domain.model.Account;

/**
 * DOmain port - no framework dependencies,
 * Infrastructure layer provides the adapter implementation
 */
public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(UUID id);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);
}
