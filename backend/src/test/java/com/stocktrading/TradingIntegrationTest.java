package com.stocktrading;

import com.stocktrading.dto.PlaceOrderRequest;
import com.stocktrading.entity.*;
import com.stocktrading.exception.ErrorCode;
import com.stocktrading.repository.*;
import com.stocktrading.security.JwtService;
import com.stocktrading.service.PortfolioService;
import com.stocktrading.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:trading-integration;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TradingIntegrationTest {
    @Autowired TradingService trading;
    @Autowired PortfolioService portfolio;
    @Autowired UserRepository users;
    @Autowired StockRepository stocks;
    @Autowired PositionRepository positions;
    @Autowired OrderRepository orders;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactionManager;

    private record Market(Long buyer, Long seller, Long stock) {}

    private Market market() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            User buyer = user("buyer_" + suffix, "10000");
            User seller = user("seller_" + suffix, "0");
            Stock stock = new Stock();
            stock.setCode(suffix);
            stock.setName("并发测试");
            stock.setCurrentPrice(new BigDecimal("10"));
            stock = stocks.save(stock);
            UserPosition position = new UserPosition(seller.getId(), stock.getId());
            position.setNumFree(100);
            positions.save(position);
            return new Market(buyer.getId(), seller.getId(), stock.getId());
        });
    }

    private User user(String login, String cash) {
        User user = new User();
        user.setName(login);
        user.setLoginName(login);
        user.setPasswd("test-only");
        user.setCnyFree(new BigDecimal(cash));
        return users.save(user);
    }

    private PlaceOrderRequest request(Long stock, OrderType type, int amount) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setStockId(stock);
        request.setType(type);
        request.setPrice(new BigDecimal("10.00"));
        request.setAmount(amount);
        return request;
    }

    @Test
    void simultaneousOppositeOrdersSettleWithoutLeavingCrossedOrders() throws Exception {
        Market market = market();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 20; i++) {
                boolean buy = i % 2 == 0;
                futures.add(executor.submit(() -> {
                    start.await();
                    return trading.placeOrder(buy ? market.buyer : market.seller,
                            request(market.stock, buy ? OrderType.BUY : OrderType.SELL, 10));
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        }
        assertThat(orders.findOpenOrdersByStock(market.stock)).isEmpty();
        assertThat(users.findById(market.buyer).orElseThrow().getCnyFree()).isEqualByComparingTo("9000");
        assertThat(users.findById(market.seller).orElseThrow().getCnyFree()).isEqualByComparingTo("1000");
        assertThat(users.findById(market.buyer).orElseThrow().getCnyFrozen()).isZero();
        assertThat(positions.findByUserIdAndStockId(market.buyer, market.stock).orElseThrow().getTotal()).isEqualTo(100);
        assertThat(positions.findByUserIdAndStockId(market.seller, market.stock).orElseThrow().getTotal()).isZero();
    }

    @Test
    void cancelRacingWithMatchConservesCashAndShares() throws Exception {
        Market market = market();
        Long sellId = trading.placeOrder(market.seller, request(market.stock, OrderType.SELL, 100)).getOrderId();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<?> cancel = executor.submit(() -> {
                start.await();
                trading.cancelOrder(market.seller, sellId);
                return null;
            });
            Future<?> buy = executor.submit(() -> {
                start.await();
                return trading.placeOrder(market.buyer, request(market.stock, OrderType.BUY, 60));
            });
            start.countDown();
            cancel.get(20, TimeUnit.SECONDS);
            buy.get(20, TimeUnit.SECONDS);
        }
        Order sell = orders.findById(sellId).orElseThrow();
        assertThat(sell.getCanceled()).isTrue();
        assertThat(sell.getDealed()).isIn(0, 60);
        User buyer = users.findById(market.buyer).orElseThrow();
        User seller = users.findById(market.seller).orElseThrow();
        assertThat(buyer.getCnyFree().add(buyer.getCnyFrozen()).add(seller.getCnyFree()).add(seller.getCnyFrozen()))
                .isEqualByComparingTo("10000");
        int buyerShares = positions.findByUserIdAndStockId(market.buyer, market.stock).map(UserPosition::getTotal).orElse(0);
        UserPosition sellerPosition = positions.findByUserIdAndStockId(market.seller, market.stock).orElseThrow();
        assertThat(buyerShares).isEqualTo(sell.getDealed());
        assertThat(buyerShares + sellerPosition.getTotal()).isEqualTo(100);
        assertThat(sellerPosition.getNumFrozen()).isZero();
        assertThat(seller.getCnyFree()).isEqualByComparingTo(BigDecimal.TEN.multiply(BigDecimal.valueOf(buyerShares)));
    }

    @Test
    void openOrderPaginationKeepsOlderPendingOrders() {
        Market market = market();
        Long pending = trading.placeOrder(market.buyer, request(market.stock, OrderType.BUY, 1)).getOrderId();
        for (int i = 0; i < 51; i++) {
            Long id = trading.placeOrder(market.buyer, request(market.stock, OrderType.BUY, 1)).getOrderId();
            trading.cancelOrder(market.buyer, id);
        }
        var page = portfolio.orders(market.buyer, 0, 20, true);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.content()).extracting(com.stocktrading.dto.OrderView::getId).containsExactly(pending);
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void excessiveQuantityAndNotionalAreRejected() throws Exception {
        Market market = market();
        String token = jwt.generateToken(users.findById(market.buyer).orElseThrow());
        for (String fields : new String[]{"\"price\":10,\"amount\":1000001", "\"price\":200,\"amount\":1000000"}) {
            mvc.perform(post("/api/trade/place").header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .content("{\"stockId\":" + market.stock + ",\"type\":\"BUY\"," + fields + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST));
        }
        assertThat(users.findById(market.buyer).orElseThrow().getCnyFrozen()).isZero();
        assertThat(orders.findOpenOrdersByStock(market.stock)).isEmpty();
    }

    @Test
    void malformedOrderReturnsBadRequest() throws Exception {
        String token = jwt.generateToken(users.findByLoginName("user001").orElseThrow());
        mvc.perform(post("/api/trade/place").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"stockId\":1,\"type\":\"INVALID\",\"price\":10,\"amount\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST));
    }

    @Test
    void fractionalQuantityIsRejectedInsteadOfTruncated() throws Exception {
        String token = jwt.generateToken(users.findByLoginName("user001").orElseThrow());
        mvc.perform(post("/api/trade/place").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"stockId\":1,\"type\":\"BUY\",\"price\":10,\"amount\":1.5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST));
    }
}
