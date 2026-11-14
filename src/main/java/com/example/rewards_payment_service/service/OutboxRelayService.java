package com.example.rewards_payment_service.service;

import com.example.rewards_payment_service.dto.TransactionEvent;
import com.example.rewards_payment_service.entity.OutboxEvent;
import com.example.rewards_payment_service.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxRelayService {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OutboxRelayService(OutboxEventRepository outboxRepo, KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxRepo = outboxRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Runs every 1000 milliseconds (1 second)
    @Scheduled(fixedDelay = 1000)
    public void relayEventsToKafka() {
        List<OutboxEvent> pendingEvents = outboxRepo.findByProcessedFalse();

        for (OutboxEvent event : pendingEvents) {
            // Convert the database row back into our Kafka Event DTO
            TransactionEvent payload = new TransactionEvent(
                event.getIdempotencyKey(), 
                event.getAccountId(), 
                event.getAmount()
            );

            // Send to Kafka
            kafkaTemplate.send("txn.requested", event.getAccountId(), payload);

            // Mark as processed so we don't send it again
            event.setProcessed(true);
            outboxRepo.save(event);
            
            System.out.println("Relayed event to Kafka: " + event.getIdempotencyKey());
        }
    }
}