package com.multi.currency.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String accountNumber;
    private final UUID userId;
    private Money balance;
    private final String currencyCode;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
}
