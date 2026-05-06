package com.multi.currency.wallet.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import com.multi.currency.wallet.application.dto.request.DepositRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.model.Money;
import com.multi.currency.wallet.domain.model.Transaction;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse execute(DepositRequest request, UUID userId) {
        log.info("Processing deposit of {} {} to account {}",
                request.amount(), request.currencyCode(), request.accountNumber());

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.accountNumber()));

        if (!account.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this account");
        }

        Money depositAmount = Money.of(request.amount(), request.currencyCode());

        account.credit(depositAmount);
        accountRepository.save(account);

        Transaction transaction = Transaction.createDeposit(account.getId(), depositAmount, account.getBalance(),
                request.description());

        transactionRepository.save(transaction);

        log.info("Deposit Successful. Transaction ID: {}", transaction.getId());
        return TransactionResponse.from(transaction);

    }
}
