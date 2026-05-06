package com.multi.currency.wallet.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.multi.currency.wallet.application.dto.request.TransferRequest;
import com.multi.currency.wallet.application.dto.response.TransferResponse;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;
import com.multi.currency.wallet.domain.service.TransferDomainService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferDomainService transferDomainService;

    public TransferResponse execute(TransferRequest request, UUID userId) {
        return null;
    }
}
