package com.stocktrading.exception;

import org.springframework.security.core.AuthenticationException;

/** JWT 解析失败（过期、签名错误、格式非法）。 */
public class JwtAuthenticationException extends AuthenticationException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
