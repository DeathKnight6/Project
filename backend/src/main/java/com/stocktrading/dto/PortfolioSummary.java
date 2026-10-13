package com.stocktrading.dto;

import java.math.BigDecimal;

/** 资产总览。 */
public class PortfolioSummary {

    private Long userId;
    private String name;
    private String loginName;
    /** 可用资金 */
    private BigDecimal cnyFree;
    /** 冻结资金 */
    private BigDecimal cnyFrozen;
    /** 资金合计 = 可用 + 冻结 */
    private BigDecimal cashTotal;
    /** 持仓市值合计（按最新价） */
    private BigDecimal marketValue;
    /** 总资产 = 资金合计 + 持仓市值 */
    private BigDecimal totalAsset;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public BigDecimal getCnyFree() {
        return cnyFree;
    }

    public void setCnyFree(BigDecimal cnyFree) {
        this.cnyFree = cnyFree;
    }

    public BigDecimal getCnyFrozen() {
        return cnyFrozen;
    }

    public void setCnyFrozen(BigDecimal cnyFrozen) {
        this.cnyFrozen = cnyFrozen;
    }

    public BigDecimal getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(BigDecimal cashTotal) {
        this.cashTotal = cashTotal;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getTotalAsset() {
        return totalAsset;
    }

    public void setTotalAsset(BigDecimal totalAsset) {
        this.totalAsset = totalAsset;
    }
}
