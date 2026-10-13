package com.stocktrading.dto;

import com.stocktrading.entity.Order;
import com.stocktrading.entity.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单展示对象：把 stockId 补全成股票代码与名称，并给出中文状态。 */
public class OrderView {

    private Long id;
    private Long stockId;
    private String stockCode;
    private String stockName;
    private OrderType type;
    private BigDecimal price;
    /** 下单总量 */
    private Integer amount;
    /** 已成交 */
    private Integer dealed;
    /** 未成交 */
    private Integer undealed;
    private Boolean canceled;
    /** 中文状态：已成交 / 部分成交 / 未成交 / 已撤单 */
    private String status;
    private LocalDateTime createdAt;

    public static OrderView of(Order order, String stockCode, String stockName) {
        OrderView v = new OrderView();
        v.id = order.getId();
        v.stockId = order.getStockId();
        v.stockCode = stockCode;
        v.stockName = stockName;
        v.type = order.getType();
        v.price = order.getPrice();
        v.amount = order.getAmount();
        v.dealed = order.getDealed();
        v.undealed = order.getUndealed();
        v.canceled = order.getCanceled();
        v.status = resolveStatus(order);
        v.createdAt = order.getCreatedAt();
        return v;
    }

    private static String resolveStatus(Order order) {
        if (Boolean.TRUE.equals(order.getCanceled())) {
            return "已撤单";
        }
        int dealed = order.getDealed() == null ? 0 : order.getDealed();
        if (dealed == 0) {
            return "未成交";
        }
        return order.getUndealed() != null && order.getUndealed() > 0 ? "部分成交" : "已成交";
    }

    public Long getId() {
        return id;
    }

    public Long getStockId() {
        return stockId;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public OrderType getType() {
        return type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getAmount() {
        return amount;
    }

    public Integer getDealed() {
        return dealed;
    }

    public Integer getUndealed() {
        return undealed;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
