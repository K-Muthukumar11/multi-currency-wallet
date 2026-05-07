package com.multi.currency.wallet.domain.exception;

public class TransactionNotFoundException extends DomainException {
    public TransactionNotFoundException(String message) {
        super(message);
    }

}
