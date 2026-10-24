package com.stocktrading.service;

import com.stocktrading.entity.*;
import com.stocktrading.repository.OrderRepository;
import com.stocktrading.repository.StockRepository;
import com.stocktrading.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 撮合引擎。
 * <p>规则：
 * <ol>
 *   <li>买入价 ≥ 卖出价才成交；</li>
 *   <li>价格优先（买单价高者先、卖单价低者先），同价时间优先（先挂先成交）；</li>
 *   <li>成交价取「先到一方」即挂单方的价格——新单永远吃已在委托簿里的单，所以成交价就是对手挂单价；</li>
 *   <li>成交量 = min(新单未成交, 对手未成交)，支持部分成交。</li>
 * </ol>
 * 调用方必须先取得全局交易行锁，再锁账户及订单，保证下单与撤单串行执行。
 */
@Service
public class MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final StockRepository stockRepository;
    private final AccountService accountService;

    public MatchingEngine(OrderRepository orderRepository,
                          TransactionRepository transactionRepository,
                          StockRepository stockRepository,
                          AccountService accountService) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.stockRepository = stockRepository;
        this.accountService = accountService;
    }

    /** 撮合结果。 */
    public record Outcome(int dealedAmount, BigDecimal lastPrice, List<FillDetail> fills) {
    }

    /** 单笔成交明细，用于回传给前端展示。 */
    public record FillDetail(Long transactionId, BigDecimal price, int amount) {
    }

    /**
     * 让 incoming 与委托簿中价格可接受的对手单逐笔成交，直到 incoming 吃完或没有可成交对手。
     * incoming 必须是已持久化（有 id）且在事务内的实体。
     */
    public Outcome match(Order incoming) {
        List<Order> counterparts = incoming.getType() == OrderType.BUY
                ? orderRepository.findMatchableSellOrders(incoming.getStockId(), incoming.getPrice())
                : orderRepository.findMatchableBuyOrders(incoming.getStockId(), incoming.getPrice());

        List<FillDetail> fills = new ArrayList<>();
        int dealedTotal = 0;
        BigDecimal lastPrice = null;

        for (Order counter : counterparts) {
            int remaining = incoming.getUndealed() == null ? 0 : incoming.getUndealed();
            if (remaining <= 0) {
                break;
            }
            int counterRemaining = counter.getUndealed() == null ? 0 : counter.getUndealed();
            int qty = Math.min(remaining, counterRemaining);
            if (qty <= 0) {
                continue;
            }

            // 对手单一定是先挂在簿上的那一方，故其价格即成交价
            BigDecimal tradePrice = counter.getPrice();
            Transaction tx = settle(incoming, counter, tradePrice, qty);

            fills.add(new FillDetail(tx.getId(), tradePrice, qty));
            dealedTotal += qty;
            lastPrice = tradePrice;
        }

        if (dealedTotal > 0) {
            log.info("订单 #{} 撮合成交 {} 股，最新成交价 {}", incoming.getId(), dealedTotal, lastPrice);
        }
        return new Outcome(dealedTotal, lastPrice, fills);
    }

    /**
     * 交割一笔成交：更新买卖双方的资金、持仓、订单进度，并落一条成交流水。
     * 买入方在下单时按「委托价 × 数量」冻结资金，成交时按实际成交价扣款，价差退回可用资金。
     */
    private Transaction settle(Order incoming, Order counter, BigDecimal price, int qty) {
        Order buyOrder = incoming.getType() == OrderType.BUY ? incoming : counter;
        Order sellOrder = incoming.getType() == OrderType.BUY ? counter : incoming;

        // 在交易入口锁的保护下，按 userId 升序锁定双方账户。
        boolean buyFirst = buyOrder.getUserId() <= sellOrder.getUserId();
        Long firstId = buyFirst ? buyOrder.getUserId() : sellOrder.getUserId();
        Long secondId = buyFirst ? sellOrder.getUserId() : buyOrder.getUserId();

        User first = accountService.lockUser(firstId);
        User second = accountService.lockUser(secondId);
        User buyer = buyFirst ? first : second;
        User seller = buyFirst ? second : first;

        BigDecimal quantity = BigDecimal.valueOf(qty);
        // 下单时按委托价冻结的金额
        BigDecimal reserved = buyOrder.getPrice().multiply(quantity);
        // 本次实际成交金额
        BigDecimal cost = price.multiply(quantity);

        buyer.setCnyFrozen(buyer.getCnyFrozen().subtract(reserved));
        // 委托价高于成交价的差额退回可用资金
        buyer.setCnyFree(buyer.getCnyFree().add(reserved.subtract(cost)));
        seller.setCnyFree(seller.getCnyFree().add(cost));

        UserPosition buyerPosition = accountService.lockOrCreatePosition(buyOrder.getUserId(), buyOrder.getStockId());
        buyerPosition.setNumFree(buyerPosition.getNumFree() + qty);

        UserPosition sellerPosition = accountService.lockOrCreatePosition(sellOrder.getUserId(), sellOrder.getStockId());
        // 卖单下单时已把股票从可用划到冻结，成交即从冻结中划走
        sellerPosition.setNumFrozen(sellerPosition.getNumFrozen() - qty);

        buyOrder.setUndealed(buyOrder.getUndealed() - qty);
        buyOrder.setDealed(buyOrder.getDealed() + qty);
        sellOrder.setUndealed(sellOrder.getUndealed() - qty);
        sellOrder.setDealed(sellOrder.getDealed() + qty);

        // 最新价跟随最近一笔成交
        stockRepository.findById(buyOrder.getStockId())
                .ifPresent(stock -> stock.setCurrentPrice(price));

        Transaction transaction = new Transaction();
        transaction.setBuyOrderId(buyOrder.getId());
        transaction.setSellOrderId(sellOrder.getId());
        transaction.setPrice(price);
        transaction.setAmount(qty);
        return transactionRepository.save(transaction);
    }
}
