package com.stocktrading.service;

import com.stocktrading.entity.User;
import com.stocktrading.entity.UserPosition;
import com.stocktrading.exception.BusinessException;
import com.stocktrading.exception.ErrorCode;
import com.stocktrading.repository.PositionRepository;
import com.stocktrading.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户读写的公共入口。TradingService（冻结/解冻）与 MatchingEngine（交割）都通过它拿加锁后的实体，
 * 避免两者互相依赖形成循环。
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PositionRepository positionRepository;

    public AccountService(UserRepository userRepository, PositionRepository positionRepository) {
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
    }

    /** 加写锁读取用户；必须在事务内调用。 */
    @Transactional
    public User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
    }

    /** 加写锁读取持仓，不存在则新建一条空持仓。 */
    @Transactional
    public UserPosition lockOrCreatePosition(Long userId, Long stockId) {
        return positionRepository.findForUpdate(userId, stockId)
                .orElseGet(() -> positionRepository.save(new UserPosition(userId, stockId)));
    }
}
