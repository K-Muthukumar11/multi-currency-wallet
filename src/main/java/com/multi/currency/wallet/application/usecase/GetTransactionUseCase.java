package com.multi.currency.wallet.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import java.util.UUID;
import com.multi.currency.wallet.domain.exception.AccountNotFoundException;
import java.util.List;
import com.multi.currency.wallet.domain.model.Account;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;

import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetTransactionUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> execute(String accountNumber, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return transactionRepository.findByAccountId(account.getId(), pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> executeForUser(UUID userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return accounts.stream()
                .flatMap(acc -> transactionRepository.findByAccountId(acc.getId()).stream())
                .map(TransactionResponse::from)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt())) // Sort by createdAt desc
                .toList();
    }
}
