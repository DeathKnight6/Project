package com.stocktrading.service;

import com.stocktrading.dto.StockView;
import com.stocktrading.entity.Stock;
import com.stocktrading.exception.BusinessException;
import com.stocktrading.exception.ErrorCode;
import com.stocktrading.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 股票行情。
 * <p>最新价的来源有两种：撮合成交时自动跟随成交价更新，以及管理员手工调价。
 * 这里不接真实行情，需要的话可在 {@link #listStocks()} 之上加定时任务或 WebSocket 推送。
 */
@Service
public class StockPriceService {

    private final StockRepository stockRepository;

    public StockPriceService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public List<StockView> listStocks() {
        return stockRepository.findAll().stream()
                .sorted(Comparator.comparing(Stock::getCode))
                .map(StockView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Stock requireStock(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, "股票不存在"));
    }

    @Transactional
    public StockView updatePrice(Long stockId, BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw BusinessException.badRequest("价格必须大于 0");
        }
        Stock stock = requireStock(stockId);
        stock.setCurrentPrice(price);
        return StockView.from(stockRepository.save(stock));
    }
}
