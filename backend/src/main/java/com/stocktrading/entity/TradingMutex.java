package com.stocktrading.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 单一交易入口锁。下单和撤单在获取任何账户或订单锁之前锁住此行。 */
@Entity
@Table(name = "trading_mutex")
public class TradingMutex {
    public static final long ID = 1L;

    @Id
    private Long id = ID;

    public TradingMutex() {
    }
}
