package com.example.healthexam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 签发与解析（HS256）。token 里只放 userId/username/roleCode，不放手机号等敏感信息。 */
@Component
public class JwtUtil {

    @Value("${app.jwt-secret}")
    private String secret;

    @Value("${app.jwt-expire-hours}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String create(Long userId, String username, String roleCode, Long orgId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", roleCode)
                .claim("orgId", orgId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    /** 解析失败（过期/篡改）返回 null，由过滤器判定为未登录 */
    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            LoginUser u = new LoginUser();
            u.setUserId(Long.valueOf(c.getSubject()));
            u.setUsername(c.get("username", String.class));
            u.setRoleCode(c.get("role", String.class));
            Number orgId = c.get("orgId", Number.class);
            u.setOrgId(orgId == null ? null : orgId.longValue());
            return u;
        } catch (Exception e) {
            return null;
        }
    }
}
