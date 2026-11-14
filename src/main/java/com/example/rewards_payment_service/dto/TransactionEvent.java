package com.example.rewards_payment_service.dto;

import java.math.BigDecimal;

// This record acts as the strict, immutable contract between Producer and Consumer
public record TransactionEvent(
    String idempotencyKey, 
    String accountId, 
    BigDecimal amount
) {}