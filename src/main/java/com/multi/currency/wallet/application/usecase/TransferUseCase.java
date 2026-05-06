package com.multi.currency.wallet.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.multi.currency.wallet.application.dto.request.TransferRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.application.dto.response.TransferResponse;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.Money;
import com.multi.currency.wallet.domain.model.Transaction;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;
import com.multi.currency.wallet.domain.service.TransferDomainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferDomainService transferDomainService;

    /**
     * Atomic transfer: debit + credit happen in a single DB transaction.
     * if anything fails, Postgres rolls back both account updates and both ledger
     * entries.
     */
    @Transactional
    public TransferResponse execute(TransferRequest request, UUID userId) {
        log.info("Processing transfer of {} {} from {} to {}",
                request.amount(), request.currencyCode(), request.sourceAccountNumber(),
                request.destinationAccountNumber());

        Account source = accountRepository.findByAccountNumber(request.sourceAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Source account not found: " + request.sourceAccountNumber()));

        if (!source.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Account destination = accountRepository.findByAccountNumber(request.destinationAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Destination account not found: " + request.destinationAccountNumber()));

        Money amount = Money.of(request.amount(), request.currencyCode());

        // Domain service perfoms the atomic debit + credit on aggregates
        List<Transaction> transactions = transferDomainService.transfer(source, destination, amount,
                request.description());

        // Persist both accounts (updated balances)
        accountRepository.save(source);
        accountRepository.save(destination);

        // Persist both ledger entries together
        transactionRepository.saveAll(transactions);

        Transaction debitTx = transactions.get(0);
        Transaction creditTx = transactions.get(1);

        log.info("Transfer successful. Debit TX: {}, Credit TX: {}", debitTx.getId(), creditTx.getId());
        // Return full transaction objects so callers can inspect type, balanceAfter,
        // etc.
        return new TransferResponse(
                TransactionResponse.from(debitTx),
                TransactionResponse.from(creditTx),
                source.getAccountNumber(),
                destination.getAccountNumber());
    }
}
