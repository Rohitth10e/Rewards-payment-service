package com.example.rewards_payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // 1. Where to send the message when we give up (appends ".DLT" to the original
        // topic name)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // 2. How many times to retry, and how long to wait (Wait 1 second, retry up to
        // 3 times)
        FixedBackOff backOff = new FixedBackOff(1000L, 3);

        // 3. Combine them into the error handler
        return new DefaultErrorHandler(recoverer, backOff);
    }

    // Explicitly create the main topic
    @Bean
    public NewTopic mainTopic() {
        return TopicBuilder.name("txn.requested").partitions(3).build();
    }

    // Explicitly create the Dead Letter Topic
    @Bean
    public NewTopic dltTopic() {
        return TopicBuilder.name("txn.requested-dlt").partitions(1).build();
    }
}