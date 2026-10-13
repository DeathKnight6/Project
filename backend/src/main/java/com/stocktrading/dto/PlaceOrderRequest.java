package com.stocktrading.dto;

import com.stocktrading.entity.OrderType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class PlaceOrderRequest {

    @NotNull(message = "股票不能为空")
    private Long stockId;

    @NotNull(message = "买卖方向不能为空")
    private OrderType type;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Digits(integer = 12, fraction = 2, message = "价格最多保留 2 位小数")
    private BigDecimal price;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1 股")
    private Integer amount;

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

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
