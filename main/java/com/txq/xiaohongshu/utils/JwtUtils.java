package com.txq.xiaohongshu.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 令牌工具类 — 生成与校验
 * 签名算法: HMAC-SHA256，密钥使用 Base64 编码
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * Base64 编码的密钥（至少 256 位，即 32 字节）
     */
    @Value("${jwt.secret}")
    private String base64Secret;

    /**
     * 令牌过期时间（毫秒），默认 7 天
     */
    @Value("${jwt.expiration:604800000}")
    private long expiration;

    // ──────────────────── 密钥获取 ────────────────────

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ──────────────────── 令牌生成 ────────────────────

    /**
     * 根据用户 ID 和用户名生成 JWT
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    // ──────────────────── 令牌解析 ────────────────────

    /**
     * 解析并校验令牌，返回 Claims
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims，校验失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException | MalformedJwtException
                 | SecurityException | IllegalArgumentException e) {
            log.warn("JWT 校验失败 ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    // ──────────────────── 信息提取 ────────────────────

    /**
     * 从令牌中提取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID，解析失败返回 null
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从令牌中提取用户名
     *
     * @param token JWT 字符串
     * @return 用户名，解析失败返回 null
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("username", String.class);
    }

    // ──────────────────── 状态判断 ────────────────────

    /**
     * 判断令牌是否已过期
     *
     * @param token JWT 字符串
     * @return true=已过期
     */
    public boolean isExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }

    /**
     * 校验令牌是否合法（未过期、签名正确）
     *
     * @param token JWT 字符串
     * @return true=合法
     */
    public boolean validate(String token) {
        return parseToken(token) != null && !isExpired(token);
    }
}