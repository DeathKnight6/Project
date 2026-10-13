package com.stocktrading;

import com.stocktrading.dto.PlaceOrderRequest;
import com.stocktrading.dto.TradeResult;
import com.stocktrading.entity.*;
import com.stocktrading.exception.BusinessException;
import com.stocktrading.repository.PositionRepository;
import com.stocktrading.repository.StockRepository;
import com.stocktrading.repository.TransactionRepository;
import com.stocktrading.repository.UserRepository;
import com.stocktrading.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 撮合引擎的行为验证：完全成交、部分成交、价格优先、时间优先、成交价取挂单方、冻结与撤单解冻。
 * 每个用例跑在一个事务里，结束后回滚，互不影响。
 */
@SpringBootTest
@Transactional
class MatchingEngineTest {

    @Autowired
    private TradingService tradingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long stockId;

    @BeforeEach
    void setUp() {
        // 复用 DataInitializer 预置的股票，避免每个用例重复建标的
        stockId = stockRepository.findByCode("600519").orElseThrow().getId();
    }

    @Test
    @DisplayName("买卖价相等时完全成交，资金与持仓双向结清")
    void fullMatch() {
        User buyer = newUser("t_buyer_1", "100000");
        User seller = newUser("t_seller_1", "0");
        giveShares(seller, 100);

        place(buyer, OrderType.BUY, "10.00", 100);
        TradeResult result = place(seller, OrderType.SELL, "10.00", 100);

        assertThat(result.getDealed()).isEqualTo(100);
        assertThat(result.getPending()).isZero();

        // 买方：付出 1000 元，拿到 100 股；冻结清零
        assertThat(reload(buyer).getCnyFree()).isEqualByComparingTo("99000");
        assertThat(reload(buyer).getCnyFrozen()).isEqualByComparingTo("0");
        assertThat(position(buyer).getNumFree()).isEqualTo(100);

        // 卖方：股票清空，收到 1000 元
        assertThat(reload(seller).getCnyFree()).isEqualByComparingTo("1000");
        assertThat(position(seller).getTotal()).isZero();
    }

    @Test
    @DisplayName("数量不等时部分成交，剩余部分继续挂单")
    void partialMatch() {
        User buyer = newUser("t_buyer_2", "100000");
        User seller = newUser("t_seller_2", "0");
        giveShares(seller, 60);

        place(buyer, OrderType.BUY, "10.00", 100);
        TradeResult result = place(seller, OrderType.SELL, "10.00", 60);

        assertThat(result.getDealed()).isEqualTo(60);
        assertThat(position(buyer).getNumFree()).isEqualTo(60);
        // 下单即冻结 100×10=1000 元，因此可用资金先减到 99000（成交不额外扣可用）
        assertThat(reload(buyer).getCnyFree()).isEqualByComparingTo("99000");
        // 剩余 40 股仍挂在委托簿上，对应 400 元继续冻结
        assertThat(reload(buyer).getCnyFrozen()).isEqualByComparingTo("400");
        assertThat(position(seller).getTotal()).isZero();
    }

    @Test
    @DisplayName("价格优先：买单先吃卖价更低的挂单")
    void pricePriority() {
        User buyer = newUser("t_buyer_3", "100000");
        User expensiveSeller = newUser("t_seller_3a", "0");
        User cheapSeller = newUser("t_seller_3b", "0");
        giveShares(expensiveSeller, 100);
        giveShares(cheapSeller, 100);

        // 高价卖单先挂，低价卖单后挂；成交时低价单应优先
        place(expensiveSeller, OrderType.SELL, "10.50", 100);
        place(cheapSeller, OrderType.SELL, "10.00", 100);

        TradeResult result = place(buyer, OrderType.BUY, "10.50", 100);

        assertThat(result.getDealed()).isEqualTo(100);
        assertThat(result.getFills()).hasSize(1);
        assertThat(result.getFills().get(0).price()).isEqualByComparingTo("10.00");
        // 低价卖家成交，高价卖家仍挂单
        assertThat(position(cheapSeller).getTotal()).isZero();
        assertThat(position(expensiveSeller).getTotal()).isEqualTo(100);
    }

    @Test
    @DisplayName("时间优先：同价挂单先挂先成交")
    void timePriority() {
        User buyer = newUser("t_buyer_4", "100000");
        User firstSeller = newUser("t_seller_4a", "0");
        User secondSeller = newUser("t_seller_4b", "0");
        giveShares(firstSeller, 50);
        giveShares(secondSeller, 50);

        // 两笔同价卖单，先挂的先成交
        place(firstSeller, OrderType.SELL, "10.00", 50);
        place(secondSeller, OrderType.SELL, "10.00", 50);

        TradeResult result = place(buyer, OrderType.BUY, "10.00", 50);

        assertThat(result.getDealed()).isEqualTo(50);
        assertThat(position(firstSeller).getTotal()).isZero();
        assertThat(position(secondSeller).getTotal()).isEqualTo(50);
    }

    @Test
    @DisplayName("成交价取挂单方价格：买方出价更高时按卖方挂单价成交并退还差价")
    void priceImprovementRefundedToBuyer() {
        User buyer = newUser("t_buyer_5", "100000");
        User seller = newUser("t_seller_5", "0");
        giveShares(seller, 100);

        // 卖方先挂 10.00，买方后到出价 11.00：按 10.00 成交，买方多冻结的 100 元退回
        place(seller, OrderType.SELL, "10.00", 100);
        TradeResult result = place(buyer, OrderType.BUY, "11.00", 100);

        assertThat(result.getFills()).hasSize(1);
        assertThat(result.getFills().get(0).price()).isEqualByComparingTo("10.00");
        assertThat(reload(buyer).getCnyFree()).isEqualByComparingTo("99000");
        assertThat(reload(buyer).getCnyFrozen()).isEqualByComparingTo("0");
        assertThat(reload(seller).getCnyFree()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("价格不交叉时不成交，双方挂单")
    void noMatchWhenPriceNotCrossing() {
        User buyer = newUser("t_buyer_6", "100000");
        User seller = newUser("t_seller_6", "0");
        giveShares(seller, 100);

        TradeResult buy = place(buyer, OrderType.BUY, "9.00", 100);
        TradeResult sell = place(seller, OrderType.SELL, "10.00", 100);

        assertThat(buy.getDealed()).isZero();
        assertThat(sell.getDealed()).isZero();
        assertThat(transactionRepository.count()).isZero();
        // 买单全部资金处于冻结状态
        assertThat(reload(buyer).getCnyFrozen()).isEqualByComparingTo("900");
    }

    @Test
    @DisplayName("撤单退还未成交部分的冻结资金与股票")
    void cancelRefundsFrozenAssets() {
        User buyer = newUser("t_buyer_7", "100000");
        User seller = newUser("t_seller_7", "0");
        giveShares(seller, 100);

        // 成交 40 股后撤销买单，剩余 60 股对应的 600 元应解冻
        place(seller, OrderType.SELL, "10.00", 40);
        TradeResult buy = place(buyer, OrderType.BUY, "10.00", 100);
        assertThat(buy.getDealed()).isEqualTo(40);
        assertThat(reload(buyer).getCnyFrozen()).isEqualByComparingTo("600");

        tradingService.cancelOrder(buyer.getId(), buy.getOrderId());

        assertThat(reload(buyer).getCnyFrozen()).isEqualByComparingTo("0");
        assertThat(reload(buyer).getCnyFree()).isEqualByComparingTo("99600");
        assertThat(position(buyer).getNumFree()).isEqualTo(40);
    }

    @Test
    @DisplayName("重复撤单被拒绝")
    void cancelTwiceRejected() {
        User buyer = newUser("t_buyer_8", "100000");
        TradeResult buy = place(buyer, OrderType.BUY, "9.00", 10);

        tradingService.cancelOrder(buyer.getId(), buy.getOrderId());

        assertThatThrownBy(() -> tradingService.cancelOrder(buyer.getId(), buy.getOrderId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经撤销");
    }

    @Test
    @DisplayName("资金不足时下单失败，且不产生冻结")
    void insufficientFundsRejected() {
        User poorBuyer = newUser("t_buyer_9", "100");

        assertThatThrownBy(() -> place(poorBuyer, OrderType.BUY, "10.00", 100))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("可用资金不足");

        assertThat(reload(poorBuyer).getCnyFree()).isEqualByComparingTo("100");
        assertThat(reload(poorBuyer).getCnyFrozen()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("可用股票不足时卖出失败")
    void insufficientPositionRejected() {
        User seller = newUser("t_seller_10", "0");
        giveShares(seller, 10);

        assertThatThrownBy(() -> place(seller, OrderType.SELL, "10.00", 100))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("可用股票不足");
    }

    // ---------- 测试辅助 ----------

    private User newUser(String loginName, String funds) {
        User user = new User();
        user.setName(loginName);
        user.setLoginName(loginName);
        user.setPasswd(passwordEncoder.encode("123456"));
        user.setType(User.TYPE_NORMAL);
        user.setCnyFree(new BigDecimal(funds));
        user.setCnyFrozen(BigDecimal.ZERO);
        return userRepository.save(user);
    }

    /** 直接给用户塞入可用股票，模拟其已有持仓。 */
    private void giveShares(User user, int amount) {
        UserPosition position = positionRepository
                .findByUserIdAndStockId(user.getId(), stockId)
                .orElseGet(() -> new UserPosition(user.getId(), stockId));
        position.setNumFree(position.getNumFree() + amount);
        positionRepository.save(position);
    }

    private TradeResult place(User user, OrderType type, String price, int amount) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setStockId(stockId);
        request.setType(type);
        request.setPrice(new BigDecimal(price));
        request.setAmount(amount);
        return tradingService.placeOrder(user.getId(), request);
    }

    private User reload(User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    private UserPosition position(User user) {
        return positionRepository.findByUserIdAndStockId(user.getId(), stockId).orElseThrow();
    }
}
