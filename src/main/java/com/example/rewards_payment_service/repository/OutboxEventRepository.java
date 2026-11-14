package com.example.rewards_payment_service.repository;

import com.example.rewards_payment_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    // Spring automatically writes the SQL for this method name!
    List<OutboxEvent> findByProcessedFalse(); 
}