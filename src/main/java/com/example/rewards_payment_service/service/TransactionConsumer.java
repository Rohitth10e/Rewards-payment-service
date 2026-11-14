package com.example.rewards_payment_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.example.rewards_payment_service.dto.TransactionEvent;
import com.example.rewards_payment_service.dto.TransactionRequest;
import com.example.rewards_payment_service.entity.LedgerEntry;
import com.example.rewards_payment_service.repository.AccountRepository;
import com.example.rewards_payment_service.repository.LedgerEntryRepository;
import com.example.rewards_payment_service.entity.Account;

import jakarta.transaction.Transactional;

@Service 
public class TransactionConsumer {
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository; 
    
    public TransactionConsumer(LedgerEntryRepository ledgerEntryRepository, AccountRepository accountRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
    }

    @KafkaListener(topics = "txn.requested", groupId = "rewards-group")
    @Transactional(dontRollbackOn = DataIntegrityViolationException.class)
    public void processTransaction(@Payload TransactionEvent event) {
        String idempotencyKey = event.idempotencyKey();
        String accountId = event.accountId();
        BigDecimal amount = event.amount();


        try {
            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setIdempotencyKey(idempotencyKey);
            ledgerEntry.setAccountId(accountId);
            ledgerEntry.setAmount(amount);
            ledgerEntry.setCreatedAt(LocalDateTime.now());
            ledgerEntryRepository.save(ledgerEntry);

            // 2. If successful, update the account balance
            Account acc = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
            acc.setBalance(acc.getBalance().subtract(amount));
            accountRepository.save(acc);

            System.out.println("Processed transaction for account: " + accountId + ", amount: " + amount);
        } catch (DataIntegrityViolationException e) {
            // Handle the case where the idempotency key already exists
            System.out.println("Duplicate transaction detected for idempotency key: " + idempotencyKey);
        }
    }
}
