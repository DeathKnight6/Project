package com.stocktrading.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 委托单。
 * <p>{@code undealed} 是尚未成交的数量，{@code dealed} 是已成交数量，两者之和恒等于下单数量。
 * 撤单只撤掉 undealed 的部分，已成交部分不可撤销。
 * <p>注意：类名 Order 与 {@code org.springframework.data.domain.Sort.Order} 同名，
 * 在使用 Sort 的类里请写全限定名或改用 Sort.by(...) 的字符串重载。
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_match", columnList = "stock_id,type,canceled,price,created_at")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OrderType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /** 未成交数量 */
    @Column(nullable = false)
    private Integer undealed;

    /** 已成交数量 */
    @Column(nullable = false)
    private Integer dealed = 0;

    @Column(nullable = false)
    private Boolean canceled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** 下单数量 = 未成交 + 已成交 */
    @Transient
    public int getAmount() {
        return (undealed == null ? 0 : undealed) + (dealed == null ? 0 : dealed);
    }

    /** 是否仍可参与撮合 */
    @Transient
    public boolean isOpen() {
        return !Boolean.TRUE.equals(canceled) && undealed != null && undealed > 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getUndealed() {
        return undealed;
    }

    public void setUndealed(Integer undealed) {
        this.undealed = undealed;
    }

    public Integer getDealed() {
        return dealed;
    }

    public void setDealed(Integer dealed) {
        this.dealed = dealed;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(Boolean canceled) {
        this.canceled = canceled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
