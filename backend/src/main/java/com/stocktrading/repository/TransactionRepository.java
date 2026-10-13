package com.stocktrading.repository;

import com.stocktrading.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByBuyOrderIdOrSellOrderIdOrderByCreatedAtDesc(Long buyOrderId, Long sellOrderId);
}
