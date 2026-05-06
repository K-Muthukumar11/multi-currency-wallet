package com.multi.currency.wallet.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.multi.currency.wallet.application.dto.request.DepositRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepositUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionResponse execute(DepositRequest request, UUID userId) {
        return null;
    }
}
