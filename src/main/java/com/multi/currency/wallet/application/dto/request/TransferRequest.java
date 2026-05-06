package com.multi.currency.wallet.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferRequest(
                @NotBlank(message = "Source account number is required") String sourceAccountNumber,

                @NotBlank(message = "Destination account number is required") String destinationAccountNumber,

                @NotNull(message = "Amount is required") @DecimalMin(value = "0.001", message = "Amount must be greater than 0") BigDecimal amount,

                @NotBlank(message = "Currency code is required") @Size(min = 3, max = 3, message = "currency code must be ISO 4217 currency code") String currencyCode,

                String description) {

}
