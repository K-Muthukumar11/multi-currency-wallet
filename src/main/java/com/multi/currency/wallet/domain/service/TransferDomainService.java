package com.multi.currency.wallet.domain.service;

import java.util.List;

import com.multi.currency.wallet.domain.exception.InvalidAccountOperationException;
import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.Money;
import com.multi.currency.wallet.domain.model.Transaction;

/**
 * Domain Service: encapsulates multi-account atomic transfer login=c.
 * Pure domain - no Spring, no JPA.
 */
public class TransferDomainService {

    public List<Transaction> transfer(Account source, Account destination, Money amount, String description) {
        validateTransfer(source, destination, amount);

        // Mutate domain agregates
        source.debit(amount);
        destination.credit(amount);

        // Create immutable ledger entries
        Transaction debitTx = Transaction.createTransferDebit(source.getId(), destination.getId(), amount,
                source.getBalance(), description);
        Transaction creditTx = Transaction.createTransferCredit(destination.getId(), source.getId(), amount,
                destination.getBalance(), description);
        return List.of(debitTx, creditTx);
    }

    private void validateTransfer(Account source, Account destination, Money amount) {
        if (source.getId().equals(destination.getId())) {
            throw new InvalidAccountOperationException("Cannot transer to the same account");
        }

        if (!source.getCurrencyCode().equals(destination.getCurrencyCode())) {
            throw new InvalidAccountOperationException("Cross-currency transfers are not supported. " +
                    "Source: " + source.getCurrencyCode() +
                    ", Destination: " + destination.getCurrencyCode());
        }

        if (!amount.isPositive()) {
            throw new InvalidAccountOperationException("Transfer amount must be positive");
        }

        if (!amount.getCurrencyCode().equals(source.getCurrencyCode())) {
            throw new InvalidAccountOperationException(" Amount currency does not match account currency");
        }
    }
}
