package com.stocktrading.dto;

import java.math.BigDecimal;
import java.util.List;

/** 下单结果：这单立刻成交了多少，以及产生的成交流水。 */
public class TradeResult {

    /** 本次下单生成的订单 ID */
    private Long orderId;
    /** 下单总量 */
    private Integer amount;
    /** 本次立刻成交的数量（0 表示全部挂进委托簿） */
    private Integer dealed;
    /** 剩余挂单数量 */
    private Integer pending;
    private List<Fill> fills;
    /** 面向用户的结果描述 */
    private String message;

    /** 一笔成交明细 */
    public record Fill(Long transactionId, BigDecimal price, Integer amount, String counterpartyOrderSide) {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getDealed() {
        return dealed;
    }

    public void setDealed(Integer dealed) {
        this.dealed = dealed;
    }

    public Integer getPending() {
        return pending;
    }

    public void setPending(Integer pending) {
        this.pending = pending;
    }

    public List<Fill> getFills() {
        return fills;
    }

    public void setFills(List<Fill> fills) {
        this.fills = fills;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
