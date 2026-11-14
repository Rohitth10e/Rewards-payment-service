package com.example.rewards_payment_service.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity 
@Table(name = "accounts")
public class Account {
    @Id 
    private String id;
    private BigDecimal balance;
    @Version
    private Integer version;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public BigDecimal getBalance() {
        return balance;
    }
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
