package com.english.ai.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类 — HS256签名
 * Token结构: Header.Payload.Signature
 * Payload: {userId, username, roles, exp, iat}
 */
public class JwtUtil {

    /** 密钥：生产环境必须通过 JWT_SECRET 环境变量注入。 */
    private static final String SECRET = resolveSecret();

    /** Token有效期：7天 */
    private static final long EXPIRATION_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static String resolveSecret() {
        String secret = System.getenv("JWT_SECRET");
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        return "EnglishAI-Local-Dev-JWT-Secret-Key-Change-Me-32-Bytes";
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色（ROLE_USER / ROLE_ADMIN）
     */
    public static String generate(Long userId, String username, String roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_MILLIS))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析并验证Token，返回Claims
     */
    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析并验证Token（指定密钥），Gateway等场景使用
     */
    public static Claims parse(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中提取用户ID
     */
    public static Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    /**
     * 从Token中提取用户名
     */
    public static String getUsername(String token) {
        return parse(token).get("username", String.class);
    }

    /**
     * 判断Token是否过期
     */
    public static boolean isExpired(String token) {
        try {
            parse(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
