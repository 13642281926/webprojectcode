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

@Service
public class AuthService {
    private final SecretKey secretKey;
    private final long jwtExpireMinutes;
    private final ConcurrentMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public AuthService(
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${app.auth.jwt-expire-minutes:1440}") long jwtExpireMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpireMinutes = jwtExpireMinutes;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(jwtExpireMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim("username", user.getUsername())
                .claim("nickname", user.getNickname())
                .signWith(secretKey)
                .compact();
    }

    public Long requireUserId(String authorizationHeader) {
        Claims claims = parseClaims(extractBearerToken(authorizationHeader));
        return Long.valueOf(claims.getSubject());
    }

    public void invalidateToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        Claims claims = parseClaims(token);
        revokedTokens.put(token, claims.getExpiration().toInstant());
        cleanUpRevokedTokens();
    }

    private String extractBearerToken(String header) {
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "未登录或登录已过期");
        }
        return header.substring(7);
    }

    private Claims parseClaims(String token) {
        if (revokedTokens.containsKey(token)) {
            throw new ApiException(401, "登录已失效，请重新登录");
        }
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

    private void cleanUpRevokedTokens() {
        Instant now = Instant.now();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
