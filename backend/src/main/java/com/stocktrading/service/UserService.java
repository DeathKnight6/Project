package com.stocktrading.service;

import com.stocktrading.dto.LoginRequest;
import com.stocktrading.dto.LoginResponse;
import com.stocktrading.dto.RegisterRequest;
import com.stocktrading.entity.User;
import com.stocktrading.exception.BusinessException;
import com.stocktrading.exception.ErrorCode;
import com.stocktrading.repository.UserRepository;
import com.stocktrading.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 注册、登录与密码处理。 */
@Service
public class UserService {

    /** 新注册用户的初始模拟资金 */
    public static final BigDecimal INITIAL_FUNDS = new BigDecimal("100000.0000");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String loginName = request.getLoginName().trim();
        if (userRepository.existsByLoginName(loginName)) {
            throw new BusinessException(ErrorCode.LOGIN_NAME_TAKEN, "该登录名已被占用");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setLoginName(loginName);
        // 只存 BCrypt 哈希，不落明文
        user.setPasswd(passwordEncoder.encode(request.getPasswd()));
        user.setType(User.TYPE_NORMAL);
        user.setCnyFree(INITIAL_FUNDS);
        user.setCnyFrozen(BigDecimal.ZERO);
        user = userRepository.save(user);

        return toLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginName(request.getLoginName().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPasswd(), user.getPasswd())) {
            // 与「用户不存在」返回同样的提示，避免暴露哪些登录名已注册
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }

        return toLoginResponse(user);
    }

    /** 重新签发 token，用于前端刷新用户信息 */
    @Transactional(readOnly = true)
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
    }

    private LoginResponse toLoginResponse(User user) {
        return new LoginResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getName(),
                user.getLoginName(),
                user.getType());
    }
}
