package com.multi.currency.wallet.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multi.currency.wallet.application.dto.request.DepositRequest;
import com.multi.currency.wallet.application.dto.request.ReversalRequest;
import com.multi.currency.wallet.application.dto.request.TransferRequest;
import com.multi.currency.wallet.application.dto.response.TransactionResponse;
import com.multi.currency.wallet.application.dto.response.TransferResponse;
import com.multi.currency.wallet.application.usecase.DepositUseCase;
import com.multi.currency.wallet.application.usecase.GetTransactionUseCase;
import com.multi.currency.wallet.application.usecase.ReversalUseCase;
import com.multi.currency.wallet.application.usecase.TransferUseCase;
import com.multi.currency.wallet.domain.model.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final DepositUseCase depositUseCase;
    private final TransferUseCase transferUseCase;
    private final GetTransactionUseCase getTransactionsUseCase;
    private final ReversalUseCase reversalUseCase;

    @PostMapping("/accounts/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.getId();

        return ResponseEntity.status(HttpStatus.CREATED).body(depositUseCase.execute(request, userId));
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.getId();

        return ResponseEntity.status(HttpStatus.CREATED).body(transferUseCase.execute(request, userId));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransaction(@PathVariable String accountNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(getTransactionsUseCase.execute(accountNumber, pageable));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.getId();
        return ResponseEntity.ok(getTransactionsUseCase.executeForUser(userId));
    }

    /**
     * POST /api/v1/transactions/reverse
     * Create a reversal transaction that references an original transaction ID.
     * The original transaction is NOT modified (immutability rule).
     * A new REVERSAL_DEBIT or REVERSAL_CREDIT entry is created in the ledger.
     * Each original transaction may only be reversed once.
     */
    @PostMapping("/transactions/reverse")
    public ResponseEntity<List<TransactionResponse>> reverse(
            @Valid @RequestBody ReversalRequest request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<TransactionResponse> reversals = reversalUseCase.execute(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reversals);
    }

    /**
     * POST /api/v1/transactions/reverse-transfer/{transactionId}
     * Convenience endpoint: atomically reverse both legs of a transfer
     * given either the debit or credit leg transaction ID.
     */
    @PostMapping("/transactions/reverse-transfer/{transactionId}")
    public ResponseEntity<List<TransactionResponse>> reverseTransfer(
            @PathVariable UUID transactionId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<TransactionResponse> reversals = reversalUseCase.reverseTransfer(transactionId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reversals);
    }

    /**
     * DELETE /api/v1/transactions/{id}
     * Transactions are immutable — deletion is explicitly not supported.
     * Returns 405 Method Not Allowed.
     */
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
