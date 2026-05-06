package com.multi.currency.wallet.domain.exception;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
