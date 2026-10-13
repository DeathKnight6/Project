package com.stocktrading.dto;

import com.stocktrading.entity.UserPosition;

import java.math.BigDecimal;

public class PositionView {

    private Long stockId;
    private String stockCode;
    private String stockName;
    /** 可用股数 */
    private Integer numFree;
    /** 冻结股数 */
    private Integer numFrozen;
    /** 总持仓 */
    private Integer total;
    /** 最新价 */
    private BigDecimal currentPrice;
    /** 市值 = 总持仓 × 最新价 */
    private BigDecimal marketValue;

    public static PositionView of(UserPosition p, String code, String name, BigDecimal currentPrice) {
        PositionView v = new PositionView();
        v.stockId = p.getStockId();
        v.stockCode = code;
        v.stockName = name;
        v.numFree = p.getNumFree();
        v.numFrozen = p.getNumFrozen();
        v.total = p.getTotal();
        v.currentPrice = currentPrice;
        v.marketValue = currentPrice == null
                ? BigDecimal.ZERO
                : currentPrice.multiply(BigDecimal.valueOf(v.total));
        return v;
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

    public Integer getNumFree() {
        return numFree;
    }

    public Integer getNumFrozen() {
        return numFrozen;
    }

    public Integer getTotal() {
        return total;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }
}
