package com.stocktrading.controller;

import com.stocktrading.dto.ApiResponse;
import com.stocktrading.dto.OrderView;
import com.stocktrading.dto.PageResult;
import com.stocktrading.dto.PortfolioSummary;
import com.stocktrading.dto.PositionView;
import com.stocktrading.security.AuthUser;
import com.stocktrading.service.PortfolioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/summary")
    public ApiResponse<PortfolioSummary> summary(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(portfolioService.summary(authUser.userId()));
    }

    @GetMapping("/positions")
    public ApiResponse<List<PositionView>> positions(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(portfolioService.positions(authUser.userId()));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<OrderView>> orders(@AuthenticationPrincipal AuthUser authUser,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "50") int size,
                                                     @RequestParam(defaultValue = "false") boolean openOnly) {
        return ApiResponse.ok(portfolioService.orders(authUser.userId(), page, size, openOnly));
    }
}
