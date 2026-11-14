package com.example.rewards_payment_service.repository;

import com.example.rewards_payment_service.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {}