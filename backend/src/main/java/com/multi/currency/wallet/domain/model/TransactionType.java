package com.multi.currency.wallet.domain.model;

public enum TransactionType {
    DEPOSIT(false),
    WITHDRAWAL(true),
    TRANSFER_DEBIT(true),
    TRANSFER_CREDIT(false),
    REVERSAL_DEBIT(true),
    REVERSAL_CREDIT(false);

    private final boolean debit;

    TransactionType(boolean debit) {
        this.debit = debit;
    }

    public boolean isDebit() {
        return debit;
    }

    public boolean isCredit() {
        return !debit;
    }
}
