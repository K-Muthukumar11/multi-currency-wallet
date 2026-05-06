package com.multi.currency.wallet.domain.exception;

public class AlreadyReversedException extends DomainException {
    public AlreadyReversedException(String message) {
        super(message);
    }
}
