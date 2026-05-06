package com.multi.currency.wallet.application.usecase;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetTransactionUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public Page<TransactionResponse> execute(String accountNumber, Pageable pageable) {
        return null;
    }

    public List<TransactionResponse> executeForUser(UUID userId) {
        return null;
    }

}
