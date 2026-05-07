package com.multi.currency.wallet.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.AccountStatus;

public record AccountResponse(
        UUID id,
        String accountNumber,
        UUID userId,
        BigDecimal balance,
        String currencyCode,
        AccountStatus status,
        Instant createdAt) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getUserId(),
                account.getBalance().getAmount(),
                account.getCurrencyCode(),
                account.getStatus(),
                account.getCreatedAt());
    }

}
