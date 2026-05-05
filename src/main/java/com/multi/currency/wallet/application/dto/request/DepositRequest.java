package com.multi.currency.wallet.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositRequest(
        @NotBlank(message = "Account number is required") String accountNumber,

        @NotNull(message = "Amount is required") @DecimalMin(value = "0.001", message = "Amount must be greater than 0") BigDecimal amount,

        @NotBlank(message = "Currency code is required") String currencyCode,

        String description) {
}
