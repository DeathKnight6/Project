package com.stocktrading.dto;

import com.stocktrading.entity.Stock;

import java.math.BigDecimal;

public class StockView {

    private Long id;
    private String code;
    private String name;
    private BigDecimal currentPrice;

    public static StockView from(Stock stock) {
        StockView v = new StockView();
        v.id = stock.getId();
        v.code = stock.getCode();
        v.name = stock.getName();
        v.currentPrice = stock.getCurrentPrice();
        return v;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
}
