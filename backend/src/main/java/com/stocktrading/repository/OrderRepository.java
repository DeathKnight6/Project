package com.stocktrading.repository;

import com.stocktrading.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 找出能和新买单（价格 buyPrice）撮合的卖单：价格 <= 买价。
     * 排序即撮合优先级：价格优先（低者先），同价时间优先（早者先）。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from Order o
            where o.stockId = :stockId
              and o.type = com.stocktrading.entity.OrderType.SELL
              and o.canceled = false
              and o.undealed > 0
              and o.price <= :buyPrice
            order by o.price asc, o.createdAt asc, o.id asc
            """)
    List<Order> findMatchableSellOrders(@Param("stockId") Long stockId,
                                        @Param("buyPrice") BigDecimal buyPrice);

    /**
     * 找出能和新卖单（价格 sellPrice）撮合的买单：价格 >= 卖价。
     * 排序：价格优先（高者先），同价时间优先（早者先）。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from Order o
            where o.stockId = :stockId
              and o.type = com.stocktrading.entity.OrderType.BUY
              and o.canceled = false
              and o.undealed > 0
              and o.price >= :sellPrice
            order by o.price desc, o.createdAt asc, o.id asc
            """)
    List<Order> findMatchableBuyOrders(@Param("stockId") Long stockId,
                                       @Param("sellPrice") BigDecimal sellPrice);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByUserIdAndStockIdAndTypeAndCanceledFalseAndUndealedGreaterThan(
            Long userId, Long stockId, com.stocktrading.entity.OrderType type, Integer undealed);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    /** 某只股票当前未成交的挂单，用于交易深度展示 */
    @Query("""
            select o from Order o
            where o.stockId = :stockId and o.canceled = false and o.undealed > 0
            order by o.type asc, o.price desc
            """)
    List<Order> findOpenOrdersByStock(@Param("stockId") Long stockId);
}
