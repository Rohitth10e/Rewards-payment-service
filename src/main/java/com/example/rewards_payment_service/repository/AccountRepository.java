package com.example.rewards_payment_service.repository;

import com.example.rewards_payment_service.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {}