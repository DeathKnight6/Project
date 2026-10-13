package com.stocktrading.security;

import com.stocktrading.entity.User;
import com.stocktrading.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** 生成与解析 JWT。载荷含 userId、loginName、type。 */
@Component
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration}") long expirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getLoginName())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_TYPE, user.getType())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /** 解析并校验 token；失败抛 JwtAuthenticationException。 */
    public AuthUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get(CLAIM_USER_ID, Number.class) == null
                    ? null
                    : claims.get(CLAIM_USER_ID, Number.class).longValue();
            Integer type = claims.get(CLAIM_TYPE, Number.class) == null
                    ? null
                    : claims.get(CLAIM_TYPE, Number.class).intValue();

            if (userId == null) {
                throw new JwtAuthenticationException("token 缺少 userId");
            }
            return new AuthUser(userId, claims.getSubject(), type);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtAuthenticationException("token 无效或已过期", ex);
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }
}
