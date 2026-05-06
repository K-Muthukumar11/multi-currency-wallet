package com.multi.currency.wallet.application.dto.response;

public record TransferResponse(
                TransactionResponse debitTransaction,
                TransactionResponse creditTransaction,
                String sourceAccountNumber,
                String destinationAccountNumber) {
}
