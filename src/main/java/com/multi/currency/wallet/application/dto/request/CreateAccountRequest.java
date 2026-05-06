package com.multi.currency.wallet.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
                @NotBlank(message = "Currency code is required") @Size(min = 3, max = 3, message = "Must be ISO 4217 currency code") String currencyCode) {
}
