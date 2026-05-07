package com.multi.currency.wallet.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReversalRequest(
                @NotNull(message = "Original transaction ID is required") UUID originalTransactionId,

                String reason) {
}
