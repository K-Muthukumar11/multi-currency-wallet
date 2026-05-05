package com.multi.currency.wallet.domain.exception;

public class InsufficientFundsException extends DomainException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
