package com.stocktrading.entity;

import java.io.Serializable;
import java.util.Objects;

/** UserPosition 的联合主键（userId + stockId）。 */
public class UserPositionId implements Serializable {

    private Long userId;
    private Long stockId;

    public UserPositionId() {
    }

    public UserPositionId(Long userId, Long stockId) {
        this.userId = userId;
        this.stockId = stockId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPositionId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(stockId, that.stockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, stockId);
    }
}
