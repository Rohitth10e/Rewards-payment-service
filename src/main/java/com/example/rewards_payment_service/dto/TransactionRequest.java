package com.example.rewards_payment_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record TransactionRequest(
    @NotBlank (message = "Account ID cannot be null")
    String accountId,

    @NotNull (message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount
) {}