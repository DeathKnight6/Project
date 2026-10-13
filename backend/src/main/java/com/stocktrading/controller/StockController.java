package com.stocktrading.controller;

import com.stocktrading.dto.ApiResponse;
import com.stocktrading.dto.StockView;
import com.stocktrading.service.StockPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockPriceService stockPriceService;

    public StockController(StockPriceService stockPriceService) {
        this.stockPriceService = stockPriceService;
    }

    @GetMapping("/list")
    public ApiResponse<List<StockView>> list() {
        return ApiResponse.ok(stockPriceService.listStocks());
    }
}
