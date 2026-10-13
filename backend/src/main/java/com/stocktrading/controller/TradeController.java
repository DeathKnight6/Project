package com.stocktrading.controller;

import com.stocktrading.dto.ApiResponse;
import com.stocktrading.dto.PlaceOrderRequest;
import com.stocktrading.dto.TradeResult;
import com.stocktrading.security.AuthUser;
import com.stocktrading.service.TradingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final TradingService tradingService;

    public TradeController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/place")
    public ApiResponse<TradeResult> place(@AuthenticationPrincipal AuthUser authUser,
                                          @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(tradingService.placeOrder(authUser.userId(), request));
    }

    @PostMapping("/cancel/{orderId}")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long orderId) {
        tradingService.cancelOrder(authUser.userId(), orderId);
        return ApiResponse.ok();
    }
}
