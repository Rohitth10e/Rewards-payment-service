package com.example.rewards_payment_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String idempotencyKey;
    private String accountId;
    private BigDecimal amount;
    private boolean processed = false;

    // Standard getters and setters
    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}