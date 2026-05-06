package com.multi.currency.wallet.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import com.multi.currency.wallet.application.dto.request.DepositRequest;
import com.multi.currency.wallet.application.dto.request.ReversalRequest;
import com.multi.currency.wallet.application.dto.request.TransferRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.application.dto.response.TransferResponse;

@RestController
public class TransactionController {

    public ResponseEntity<TransactionResponse> deposit(DepositRequest request, Authentication authentication) {
        return null;
    }

    public ResponseEntity<TransferResponse> transfer(TransferRequest request, Authentication authentication) {
        return null;
    }

    public ResponseEntity<Page<TransactionResponse>> getAccountTransaction(String accountNumber, Pageable pageable,
            Authentication authentication) {
        return null;
    }

    public ResponseEntity<List<TransactionResponse>> getMyTransactions(Authentication authentication) {
        return null;
    }

    public ResponseEntity<List<TransactionResponse>> reverse(ReversalRequest request, Authentication authentication) {
        return null;
    }

    public ResponseEntity<List<TransactionResponse>> reverseTransfer(UUID transactionId,
            Authentication authentication) {
        return null;
    }

    public ResponseEntity<Void> deleteTransaction(UUID id) {
        return null;
    }
}
