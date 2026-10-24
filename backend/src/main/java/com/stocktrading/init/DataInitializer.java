package com.stocktrading.init;

import com.stocktrading.entity.Stock;
import com.stocktrading.entity.User;
import com.stocktrading.entity.UserPosition;
import com.stocktrading.entity.TradingMutex;
import com.stocktrading.repository.TradingMutexRepository;
import com.stocktrading.repository.PositionRepository;
import com.stocktrading.repository.StockRepository;
import com.stocktrading.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 启动时灌入演示数据：仅在对应表为空时执行，重启不会重复插入。 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TradingMutexRepository tradingMutexRepository;

    public DataInitializer(UserRepository userRepository,
                           StockRepository stockRepository,
                           PositionRepository positionRepository,
                           PasswordEncoder passwordEncoder,
                           TradingMutexRepository tradingMutexRepository) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.positionRepository = positionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tradingMutexRepository = tradingMutexRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!tradingMutexRepository.existsById(TradingMutex.ID)) {
            tradingMutexRepository.save(new TradingMutex());
        }
        initUsers();
        initStocks();
        initPositions();
    }

    private void initUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(newUser("普通用户", "user001", "123456", User.TYPE_NORMAL, "100000"));
        userRepository.save(newUser("管理员", "admin", "admin123", User.TYPE_ADMIN, "1000000"));
        log.info("已初始化默认账号：user001/123456（普通用户）、admin/admin123（管理员）");
    }

    private User newUser(String name, String loginName, String rawPassword, int type, String funds) {
        User user = new User();
        user.setName(name);
        user.setLoginName(loginName);
        user.setPasswd(passwordEncoder.encode(rawPassword));
        user.setType(type);
        user.setCnyFree(new BigDecimal(funds));
        user.setCnyFrozen(BigDecimal.ZERO);
        return user;
    }

    private void initStocks() {
        if (stockRepository.count() > 0) {
            return;
        }
        stockRepository.saveAll(List.of(
                newStock("600519", "贵州茅台", "1680.00"),
                newStock("000001", "平安银行", "11.50"),
                newStock("600036", "招商银行", "35.80"),
                newStock("601318", "中国平安", "48.60"),
                newStock("300750", "宁德时代", "186.30")));
        log.info("已初始化 5 只股票");
    }

    private Stock newStock(String code, String name, String price) {
        Stock stock = new Stock();
        stock.setCode(code);
        stock.setName(name);
        stock.setCurrentPrice(new BigDecimal(price));
        return stock;
    }

    /**
     * 给演示账号预置底仓。
     * <p>卖出必须先持有股票，若所有账号都从零持仓起步，通过界面永远无法产生一笔成交，
     * 撮合逻辑也就无从演示。管理员持有全部 5 只股票，方便充当任意标的的对手盘。
     */
    private void initPositions() {
        if (positionRepository.count() > 0) {
            return;
        }
        User user = userRepository.findByLoginName("user001").orElseThrow();
        User admin = userRepository.findByLoginName("admin").orElseThrow();

        // 普通用户：2 只股票，合计市值约 170 万
        grantShares(user, "600519", 1000);
        grantShares(user, "000001", 2000);

        // 管理员：5 只股票各 1000 股，可对任意标的挂出卖单
        for (String code : List.of("600519", "000001", "600036", "601318", "300750")) {
            grantShares(admin, code, 1000);
        }

        log.info("已为演示账号预置底仓：user001 持有 2 只股票，admin 持有 5 只股票各 1000 股");
    }

    private void grantShares(User user, String stockCode, int amount) {
        Stock stock = stockRepository.findByCode(stockCode).orElseThrow();
        UserPosition position = new UserPosition(user.getId(), stock.getId());
        position.setNumFree(amount);
        position.setNumFrozen(0);
        positionRepository.save(position);
    }
}
