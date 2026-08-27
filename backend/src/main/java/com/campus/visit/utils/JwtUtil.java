package com.campus.visit.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：生成 / 解析 / 校验 Token
 *
 * JWT（JSON Web Token）原理：
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  header.payload.signature                              │
 *   │  header   = {"alg":"HS256","typ":"JWT"}                │
 *   │  payload  = {userId, role, isSuper, username, iat, exp} │
 *   │  signature = HMAC-SHA256(base64(header)+"."+base64(payload), secret) │
 *   └─────────────────────────────────────────────────────────┘
 *   服务端用 secret 签名 → 客户端存 token → 每次请求带上 → 服务端用 secret 验签
 *   secret 只存在服务端，客户端无法伪造签名
 *
 * 载荷字段（对标 architecture.md 第八章）：
 *   userId    用户主键
 *   role      "visitor" / "admin"
 *   isSuper   是否超管（仅 admin 有意义，boolean）
 *   username  用户名
 *   iat       签发时间（标准字段）
 *   exp       过期时间（标准字段）
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${campus.jwt.secret}")
    private String secret;

    @Value("${campus.jwt.expiration}")
    private long expiration;

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param role     角色 "visitor" 或 "admin"
     * @param isSuper  是否超管（仅 admin 有意义）
     * @param username 用户名
     * @return 签好名的 JWT 字符串
     */
    public String generateToken(Long userId, String role, Boolean isSuper, String username) {
        // 把业务信息放进 payload
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("isSuper", isSuper);
        claims.put("username", username);

        // 签发时间和过期时间
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expireAt = new Date(now + expiration);

        // 用 HS256 算法签名
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expireAt)
                .signWith(key)  // jjwt 0.12.x 会自动推断算法
                .compact();
    }

    /**
     * 解析 Token（不校验过期 —— 过期了也能拿到载荷，判断过期在 verify 里做）
     *
     * @param token JWT 字符串
     * @return 载荷 Claims，失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 过期了也能拿到载荷（e.getClaims()），但这里统一返回 null，让调用方知道验证失败
            log.warn("JWT 已过期: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.warn("JWT 签名无效（secret 不匹配）: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("JWT 格式错误: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT 类型: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("JWT 解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 完整校验：签名 + 过期
     *
     * @param token JWT 字符串
     * @return true=有效，false=无效/过期/伪造
     */
    public boolean verify(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        // 再检查一次过期（parseToken 里 ExpiredJwtException 被捕获了）
        return claims.getExpiration().after(new Date());
    }

    /**
     * 快速获取载荷字段 —— 实际业务用 UserContext 会更方便
     */
    public Long getUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public Boolean getIsSuper(Claims claims) {
        return claims.get("isSuper", Boolean.class);
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }
}
