package com.stocktrading.repository;

import com.stocktrading.entity.UserPosition;
import com.stocktrading.entity.UserPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<UserPosition, UserPositionId> {

    Optional<UserPosition> findByUserIdAndStockId(Long userId, Long stockId);

    List<UserPosition> findByUserId(Long userId);

    /** 加写锁读取持仓，避免并发改动同一行 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPosition p where p.userId = :userId and p.stockId = :stockId")
    Optional<UserPosition> findForUpdate(@Param("userId") Long userId, @Param("stockId") Long stockId);
}
