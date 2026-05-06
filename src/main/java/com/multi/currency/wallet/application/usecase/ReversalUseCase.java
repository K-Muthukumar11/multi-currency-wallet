package com.multi.currency.wallet.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.multi.currency.wallet.application.dto.request.ReversalRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.domain.repository.AccountRepository;
import com.multi.currency.wallet.domain.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReversalUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<TransactionResponse> execute(ReversalRequest request, UUID requestingUserId) {
        return null;
    }

    public List<TransactionResponse> reverseTransfer(UUID transactionId, UUID requestingUserId) {
        return null;
    }

}
