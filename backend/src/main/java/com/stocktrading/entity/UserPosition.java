package com.stocktrading.entity;

import jakarta.persistence.*;

/**
 * 持仓。按「用户 + 股票」唯一。
 * 卖出下单时把 numFree 划到 numFrozen，成交/撤单时结算。
 */
@Entity
@Table(name = "user_position")
@IdClass(UserPositionId.class)
public class UserPosition {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    /** 可用股数 */
    @Column(name = "num_free", nullable = false)
    private Integer numFree = 0;

    /** 冻结股数 */
    @Column(name = "num_frozen", nullable = false)
    private Integer numFrozen = 0;

    public UserPosition() {
    }

    public UserPosition(Long userId, Long stockId) {
        this.userId = userId;
        this.stockId = stockId;
    }

    /** 总持仓 = 可用 + 冻结 */
    @Transient
    public int getTotal() {
        return (numFree == null ? 0 : numFree) + (numFrozen == null ? 0 : numFrozen);
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

    public Integer getNumFree() {
        return numFree;
    }

    public void setNumFree(Integer numFree) {
        this.numFree = numFree;
    }

    public Integer getNumFrozen() {
        return numFrozen;
    }

    public void setNumFrozen(Integer numFrozen) {
        this.numFrozen = numFrozen;
    }
}
