package com.stocktrading.service;

import com.stocktrading.dto.OrderView;
import com.stocktrading.dto.PageResult;
import com.stocktrading.dto.PortfolioSummary;
import com.stocktrading.dto.PositionView;
import com.stocktrading.entity.Order;
import com.stocktrading.entity.Stock;
import com.stocktrading.entity.User;
import com.stocktrading.entity.UserPosition;
import com.stocktrading.repository.OrderRepository;
import com.stocktrading.repository.PositionRepository;
import com.stocktrading.repository.StockRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 资产汇总、持仓与订单查询。 */
@Service
public class PortfolioService {

    private static final int MAX_PAGE_SIZE = 200;

    private final UserService userService;
    private final PositionRepository positionRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;

    public PortfolioService(UserService userService,
                            PositionRepository positionRepository,
                            StockRepository stockRepository,
                            OrderRepository orderRepository) {
        this.userService = userService;
        this.positionRepository = positionRepository;
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
    }

    /** 资产总览：资金 + 持仓市值。 */
    @Transactional(readOnly = true)
    public PortfolioSummary summary(Long userId) {
        User user = userService.requireUser(userId);
        List<PositionView> positions = positions(userId);

        BigDecimal marketValue = positions.stream()
                .map(PositionView::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashTotal = user.getCnyFree().add(user.getCnyFrozen());

        PortfolioSummary summary = new PortfolioSummary();
        summary.setUserId(user.getId());
        summary.setName(user.getName());
        summary.setLoginName(user.getLoginName());
        summary.setCnyFree(user.getCnyFree());
        summary.setCnyFrozen(user.getCnyFrozen());
        summary.setCashTotal(cashTotal);
        summary.setMarketValue(marketValue);
        summary.setTotalAsset(cashTotal.add(marketValue));
        return summary;
    }

    /** 持仓列表。已清空的持仓（总数为 0）不返回。 */
    @Transactional(readOnly = true)
    public List<PositionView> positions(Long userId) {
        List<UserPosition> positions = positionRepository.findByUserId(userId).stream()
                .filter(p -> p.getTotal() > 0)
                .toList();
        if (positions.isEmpty()) {
            return List.of();
        }

        Map<Long, Stock> stocks = stockRepository.findAllById(
                        positions.stream().map(UserPosition::getStockId).toList()).stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return positions.stream()
                .map(p -> {
                    Stock stock = stocks.get(p.getStockId());
                    return PositionView.of(p,
                            stock == null ? "-" : stock.getCode(),
                            stock == null ? "未知股票" : stock.getName(),
                            stock == null ? BigDecimal.ZERO : stock.getCurrentPrice());
                })
                .toList();
    }

    /** 订单列表，按下单时间倒序分页。 */
    @Transactional(readOnly = true)
    public PageResult<OrderView> orders(Long userId, int page, int size) {
        return orders(userId, page, size, false);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderView> orders(Long userId, int page, int size, boolean openOnly) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Map<Long, Stock> stocks = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        return PageResult.from(
                openOnly
                        ? orderRepository.findByUserIdAndCanceledFalseAndUndealedGreaterThanOrderByCreatedAtDescIdDesc(
                                userId, 0, PageRequest.of(safePage, safeSize))
                        : orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(safePage, safeSize)),
                (Order order) -> {
                    Stock stock = stocks.get(order.getStockId());
                    return OrderView.of(order,
                            stock == null ? "-" : stock.getCode(),
                            stock == null ? "未知股票" : stock.getName());
                });
    }
}
