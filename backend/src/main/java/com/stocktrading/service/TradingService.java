package com.stocktrading.service;

import com.stocktrading.dto.PlaceOrderRequest;
import com.stocktrading.dto.TradeResult;
import com.stocktrading.entity.*;
import com.stocktrading.exception.BusinessException;
import com.stocktrading.exception.ErrorCode;
import com.stocktrading.repository.OrderRepository;
import com.stocktrading.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 下单与撤单。负责资金/股票的冻结与解冻，撮合交给 MatchingEngine。 */
@Service
public class TradingService {

    private static final Logger log = LoggerFactory.getLogger(TradingService.class);

    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final AccountService accountService;
    private final MatchingEngine matchingEngine;

    public TradingService(OrderRepository orderRepository,
                          StockRepository stockRepository,
                          AccountService accountService,
                          MatchingEngine matchingEngine) {
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
        this.accountService = accountService;
        this.matchingEngine = matchingEngine;
    }

    /** 下单：先冻结资金或股票，落单后立刻尝试撮合。 */
    @Transactional
    public TradeResult placeOrder(Long userId, PlaceOrderRequest request) {
        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, "股票不存在"));

        int amount = request.getAmount();
        BigDecimal price = request.getPrice().setScale(2, RoundingMode.HALF_UP);
        User user = accountService.lockUser(userId);

        if (request.getType() == OrderType.BUY) {
            BigDecimal required = price.multiply(BigDecimal.valueOf(amount));
            if (user.getCnyFree().compareTo(required) < 0) {
                throw BusinessException.insufficientFunds(
                        "可用资金不足，需要 " + required + " 元，当前可用 " + user.getCnyFree() + " 元");
            }
            user.setCnyFree(user.getCnyFree().subtract(required));
            user.setCnyFrozen(user.getCnyFrozen().add(required));
        } else {
            UserPosition position = accountService.lockOrCreatePosition(userId, stock.getId());
            if (position.getNumFree() < amount) {
                throw BusinessException.insufficientPosition(
                        "可用股票不足，需要 " + amount + " 股，当前可用 " + position.getNumFree() + " 股");
            }
            position.setNumFree(position.getNumFree() - amount);
            position.setNumFrozen(position.getNumFrozen() + amount);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStockId(stock.getId());
        order.setType(request.getType());
        order.setPrice(price);
        order.setUndealed(amount);
        order.setDealed(0);
        order.setCanceled(false);
        order = orderRepository.save(order);

        MatchingEngine.Outcome outcome = matchingEngine.match(order);

        TradeResult result = new TradeResult();
        result.setOrderId(order.getId());
        result.setAmount(amount);
        result.setDealed(outcome.dealedAmount());
        result.setPending(order.getUndealed());
        result.setFills(outcome.fills().stream()
                .map(f -> new TradeResult.Fill(f.transactionId(), f.price(), f.amount(),
                        request.getType() == OrderType.BUY ? "SELL" : "BUY"))
                .toList());
        result.setMessage(buildMessage(amount, outcome));
        return result;
    }

    private String buildMessage(int amount, MatchingEngine.Outcome outcome) {
        if (outcome.dealedAmount() == 0) {
            return "已挂单，等待对手方成交";
        }
        if (outcome.dealedAmount() >= amount) {
            return "全部成交，成交均价 " + averagePrice(outcome.fills());
        }
        return "部分成交 " + outcome.dealedAmount() + " 股，剩余 " + (amount - outcome.dealedAmount()) + " 股继续挂单";
    }

    private BigDecimal averagePrice(List<MatchingEngine.FillDetail> fills) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (MatchingEngine.FillDetail fill : fills) {
            totalAmount = totalAmount.add(fill.price().multiply(BigDecimal.valueOf(fill.amount())));
            totalQty = totalQty.add(BigDecimal.valueOf(fill.amount()));
        }
        return totalQty.signum() == 0
                ? BigDecimal.ZERO
                : totalAmount.divide(totalQty, 2, RoundingMode.HALF_UP);
    }

    /** 撤单：把未成交部分对应的冻结资金或股票退回可用。已成交部分不可撤。 */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        // 先无锁读一次拿到 userId，保证加锁顺序与 placeOrder 一致（用户 -> 订单）
        Order snapshot = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        if (!snapshot.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权撤销他人的订单");
        }

        User user = accountService.lockUser(userId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));

        if (Boolean.TRUE.equals(order.getCanceled())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE, "订单已经撤销过了");
        }
        int pending = order.getUndealed() == null ? 0 : order.getUndealed();
        if (pending <= 0) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE, "订单已全部成交，无法撤销");
        }

        order.setCanceled(true);

        if (order.getType() == OrderType.BUY) {
            BigDecimal refund = order.getPrice().multiply(BigDecimal.valueOf(pending));
            user.setCnyFrozen(user.getCnyFrozen().subtract(refund));
            user.setCnyFree(user.getCnyFree().add(refund));
            log.info("撤单 #{}：退回冻结资金 {} 元", orderId, refund);
        } else {
            UserPosition position = accountService.lockOrCreatePosition(userId, order.getStockId());
            position.setNumFrozen(position.getNumFrozen() - pending);
            position.setNumFree(position.getNumFree() + pending);
            log.info("撤单 #{}：退回冻结股票 {} 股", orderId, pending);
        }
    }
}
