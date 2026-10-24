package com.stocktrading.repository;

import com.stocktrading.entity.TradingMutex;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TradingMutexRepository extends JpaRepository<TradingMutex, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from TradingMutex m where m.id = 1")
    Optional<TradingMutex> lockTrading();
}
