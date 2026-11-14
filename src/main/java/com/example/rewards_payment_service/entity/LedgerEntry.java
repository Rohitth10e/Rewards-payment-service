package com.example.rewards_payment_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name="ledger_entries")
public class LedgerEntry {
    @Id 
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String idempotencyKey;
    private String accountId;
    private java.math.BigDecimal amount;
    private LocalDateTime createdAt;

    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
