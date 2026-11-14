package com.example.rewards_payment_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.example.rewards_payment_service.dto.TransactionRequest;

import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionProducer {
    // Name of Kafka queue where messages will be sent
    private static final String TOPIC = "txn.requested";
    // Kafka template for sending messages, String is the type of the key, Object is the type of the value
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // this is the method that actually builds and sends the message. It takes the unique idempotencyKey and the TransactionRequest data.
    public void publishTransaction(String idempotencyKey, TransactionRequest transactionRequest) {
        // Create a map to hold the message data
        Map<String, Object> payload = new HashMap<>();
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("transactionRequest", transactionRequest);

        // Send the message to the Kafka topic
        kafkaTemplate.send(TOPIC, transactionRequest.accountId(), payload);
    }
    
}