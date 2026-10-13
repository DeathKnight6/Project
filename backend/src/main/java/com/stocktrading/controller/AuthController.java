package com.stocktrading.controller;

import com.stocktrading.dto.ApiResponse;
import com.stocktrading.dto.LoginRequest;
import com.stocktrading.dto.LoginResponse;
import com.stocktrading.dto.RegisterRequest;
import com.stocktrading.entity.User;
import com.stocktrading.security.AuthUser;
import com.stocktrading.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    /** 前端刷新页面后用它校验 token 是否还有效，并取回最新的用户信息。 */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal AuthUser authUser) {
        User user = userService.requireUser(authUser.userId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("name", user.getName());
        data.put("loginName", user.getLoginName());
        data.put("type", user.getType());
        data.put("cnyFree", user.getCnyFree());
        data.put("cnyFrozen", user.getCnyFrozen());
        return ApiResponse.ok(data);
    }
}
