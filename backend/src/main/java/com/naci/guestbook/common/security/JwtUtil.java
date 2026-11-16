package com.naci.guestbook.common.security;

import com.naci.guestbook.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String CLAIM_USERNAME = "username";

    private static final String CLAIM_DISPLAY_NAME = "displayName";

    private final SecretKey signingKey;

    private final long expirationMs;

    /**
     * 创建 JWT 工具并校验签名密钥和过期时间配置。
     *
     * @param properties JWT 配置，包括至少 32 字节的签名密钥和令牌有效期
     * @throws IllegalArgumentException 密钥长度不足或有效期不大于 0 时抛出
     */
    public JwtUtil(JwtProperties properties) {
        byte[] secretBytes = properties.getSecret() == null
                ? new byte[0]
                : properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must contain at least 32 bytes");
        }
        if (properties.getExpirationMs() <= 0) {
            throw new IllegalArgumentException("app.jwt.expiration-ms must be greater than 0");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = properties.getExpirationMs();
    }

    /**
     * 为认证用户生成带用户 ID、用户名和展示名称的 JWT。
     *
     * @param user 认证用户
     * @return 签名后的 JWT 字符串
     * @throws IllegalArgumentException 用户为空时抛出
     */
    public String generateToken(AuthenticatedUser user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expirationMs);
        return Jwts.builder()
                .subject(user.id().toString())
                .claim(CLAIM_USERNAME, user.username())
                .claim(CLAIM_DISPLAY_NAME, user.displayName())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 使用用户 ID 和用户名生成 JWT。
     *
     * @param userId 用户 ID
     * @param username 登录用户名
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        return generateToken(new AuthenticatedUser(userId, username, username));
    }

    /**
     * 根据用户实体生成 JWT。
     *
     * @param user 用户实体
     * @return 签名后的 JWT 字符串
     * @throws IllegalArgumentException 用户为空时抛出
     */
    public String generateToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return generateToken(new AuthenticatedUser(user.getId(), user.getUsername(), user.getDisplayName()));
    }

    /**
     * 校验 JWT 签名并解析其中的认证用户信息。
     *
     * @param token JWT 字符串
     * @return 从令牌载荷还原的认证用户
     * @throws IllegalArgumentException 令牌为空时抛出
     * @throws JwtException 令牌格式错误、签名无效或已过期时抛出
     */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("JWT is missing the user id");
        }

        long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new MalformedJwtException("JWT contains an invalid user id", exception);
        }

        String username = claims.get(CLAIM_USERNAME, String.class);
        if (username == null || username.isBlank()) {
            throw new MalformedJwtException("JWT is missing the username");
        }

        String displayName = claims.get(CLAIM_DISPLAY_NAME, String.class);
        return new AuthenticatedUser(userId, username, displayName);
    }

    /**
     * 解析 JWT 并提取用户 ID。
     *
     * @param token JWT 字符串
     * @return 令牌中的用户 ID
     * @throws IllegalArgumentException 令牌为空时抛出
     * @throws JwtException 令牌无效或已过期时抛出
     */
    public Long getUserId(String token) {
        return parseToken(token).id();
    }

    /**
     * 解析 JWT 并提取过期时间。
     *
     * @param token JWT 字符串
     * @return 令牌过期时间
     * @throws IllegalArgumentException 令牌为空时抛出
     * @throws JwtException 令牌无效、已过期或缺少过期时间时抛出
     */
    public Instant getExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        if (expiration == null) {
            throw new MalformedJwtException("JWT is missing the expiration time");
        }
        return expiration.toInstant();
    }

    /**
     * 判断 JWT 是否格式正确、签名有效且未过期。
     *
     * @param token JWT 字符串
     * @return 令牌有效时返回 true，否则返回 false
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 校验 JWT 签名并返回令牌载荷。
     *
     * @param token JWT 字符串
     * @return JWT 载荷声明
     * @throws IllegalArgumentException 令牌为空时抛出
     * @throws JwtException 令牌无效或已过期时抛出
     */
    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT must not be blank");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
