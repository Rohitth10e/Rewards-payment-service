package com.example.rewards_payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rewards_payment_service.dto.TransactionRequest;
import com.example.rewards_payment_service.entity.OutboxEvent;
import com.example.rewards_payment_service.service.TransactionProducer;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionProducer producer;

    public TransactionController(TransactionProducer producer) {
        this.producer = producer;
    }

    @PostMapping 
    public ResponseEntity<Void> submitTransaction(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody TransactionRequest request) {
            OutboxEvent event = new OutboxEvent();

            event.setIdempotencyKey(idempotencyKey);
            event.setAccountId(request.accountId());
            event.setAmount(request.amount());
        
            return ResponseEntity.accepted().build(); // Return a 202 Accepted response with no body
        }
}
