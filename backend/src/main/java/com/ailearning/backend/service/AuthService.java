package com.ailearning.backend.service;

import com.ailearning.backend.entity.User;
import com.ailearning.backend.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 认证服务 —— 负责 JWT 令牌的完整生命周期管理。
 *
 * <p>核心职责：
 * <ol>
 *   <li><b>签发（issue）</b>：用户登录/注册成功后生成 JWT，有效载荷包含 userId、username、nickname、role。</li>
 *   <li><b>解析（parse）</b>：从 Authorization 请求头中提取 Bearer Token 并校验签名、有效期。</li>
 *   <li><b>校验（validate）</b>：检查令牌是否已被主动吊销（revoke），若已吊销则拒绝请求。</li>
 *   <li><b>吊销（revoke）</b>：用户登出时将令牌加入内存黑名单，并定时清理已过期的吊销记录。</li>
 * </ol>
 *
 * <p>技术选型：基于 JJWT（io.jsonwebtoken）库，使用 HMAC-SHA 对称签名算法。
 * 吊销令牌存储在 {@link ConcurrentHashMap} 中，服务重启后自动清空。
 */
@Service
public class AuthService {
    /** JWT 签名密钥，由配置中的 jwt-secret 派生 */
    private final SecretKey secretKey;
    /** JWT 过期时间（分钟），默认 1440 分钟（24 小时） */
    private final long jwtExpireMinutes;
    /** 已吊销令牌黑名单：Key = JWT 原始字符串，Value = 过期时间（用于定时清理） */
    private final ConcurrentMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    /**
     * 构造认证服务，初始化签名密钥与过期时间。
     *
     * @param jwtSecret        配置文件中的 JWT 密钥（app.auth.jwt-secret）
     * @param jwtExpireMinutes JWT 有效时长（分钟），默认 1440
     */
    public AuthService(
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${app.auth.jwt-expire-minutes:1440}") long jwtExpireMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpireMinutes = jwtExpireMinutes;
    }

    /**
     * 签发 JWT 令牌。
     *
     * <p>令牌载荷中包含 userId（sub）、username、nickname、role 四个声明。
     * 过期时间由 {@code jwtExpireMinutes} 配置决定。
     *
     * @param user 已认证的用户实体
     * @return 签发的 JWT 字符串（Bearer Token 的 token 部分）
     */
    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(jwtExpireMinutes, ChronoUnit.MINUTES);
        // 构建 JWT：主题=userId，自定义声明=username/nickname/role
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim("username", user.getUsername())
                .claim("nickname", user.getNickname())
                .claim("role", user.getRole() != null ? user.getRole() : "user")
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从请求头中解析并校验令牌，返回当前登录用户的 ID。
     *
     * <p>该方法是各业务接口鉴权的统一入口：先提取 Bearer Token，再解析校验，
     * 最后从 JWT 的 sub 声明中获取 userId。
     *
     * @param authorizationHeader HTTP 请求头 Authorization 字段的值
     * @return 当前登录用户的 ID
     * @throws ApiException 401 如果未登录或令牌无效/已过期/已吊销
     */
    public Long requireUserId(String authorizationHeader) {
        Claims claims = parseClaims(extractBearerToken(authorizationHeader));
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从请求头中提取当前用户的角色信息。
     *
     * @param authorizationHeader HTTP 请求头 Authorization 字段的值
     * @return 角色字符串（如 "admin"、"user"）
     * @throws ApiException 401 如果未登录或令牌无效
     */
    public String extractRole(String authorizationHeader) {
        Claims claims = parseClaims(extractBearerToken(authorizationHeader));
        return claims.get("role", String.class);
    }

    /**
     * 吊销（失效化）指定令牌。
     *
     * <p>将令牌加入内存黑名单，后续该令牌的所有请求都将被拒绝。
     * 吊销后自动触发过期记录清理。
     *
     * @param authorizationHeader HTTP 请求头 Authorization 字段的值
     * @throws ApiException 401 如果令牌格式不正确或无法解析
     */
    public void invalidateToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        Claims claims = parseClaims(token);
        revokedTokens.put(token, claims.getExpiration().toInstant());
        cleanUpRevokedTokens();
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token 部分。
     *
     * <p>期望格式：{@code Authorization: Bearer <token>}
     *
     * @param header Authorization 请求头的原始值
     * @return 提取出的 Token 字符串（去掉 "Bearer " 前缀）
     * @throws ApiException 401 如果 header 为空或不以 "Bearer " 开头
     */
    private String extractBearerToken(String header) {
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "未登录或登录已过期");
        }
        return header.substring(7);
    }

    /**
     * 解析并校验 JWT Token，返回其载荷声明。
     *
     * <p>两步校验：
     * <ol>
     *   <li>检查令牌是否在吊销黑名单中</li>
     *   <li>使用 JJWT 解析器验证签名并提取载荷</li>
     * </ol>
     *
     * @param token JWT 原始字符串（不含 "Bearer " 前缀）
     * @return 解析后的 JWT 声明载荷
     * @throws ApiException 401 如果令牌已吊销、签名无效或已过期
     */
    private Claims parseClaims(String token) {
        // 第一步：检查黑名单
        if (revokedTokens.containsKey(token)) {
            throw new ApiException(401, "登录已失效，请重新登录");
        }
        // 第二步：JJWT 签名校验 + 有效期校验 + 载荷提取
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new ApiException(401, "登录已失效，请重新登录");
        }
    }

    /**
     * 清理已过期令牌的吊销记录。
     *
     * <p>遍历黑名单，移除那些 JWT 过期时间已早于当前时间的记录，
     * 避免内存泄漏。在每次 {@link #invalidateToken} 时自动调用。
     */
    private void cleanUpRevokedTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
