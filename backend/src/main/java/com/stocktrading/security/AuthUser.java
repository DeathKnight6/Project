package com.stocktrading.security;

/**
 * 放入 SecurityContext 的当前用户信息。
 * Controller 里用 {@code @AuthenticationPrincipal AuthUser user} 直接取。
 */
public record AuthUser(Long userId, String loginName, Integer type) {

    public boolean isAdmin() {
        return type != null && type == 1;
    }
}
