// src/main/java/com/plateapp/plate_main/auth/security/JwtProvider.java
package com.plateapp.plate_main.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final Key key;

    @Value("${jwt.access-expire}")
    private long accessExpire;

    @Value("${jwt.refresh-expire}")
    private long refreshExpire;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 값이 비어있습니다. (최소 32바이트 이상 권장)");
        }

        // HS256은 256bit(32바이트) 이상 키가 사실상 필요함. 짧으면 Keys.hmacShaKeyFor가 예외 던짐.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 길이가 너무 짧습니다. HS256은 최소 32바이트 이상을 권장합니다.");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpire);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                // 보통 이 형태가 업데이트에도 안전함
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpire);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** ✅ 토큰 파싱: 만료/서명불일치/형식오류 등을 예외로 구분 */
    public Claims parseClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** ✅ access 토큰인지 확인까지 포함 */
    public String getUsernameFromAccessToken(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TYPE_ACCESS.equals(typ)) {
            throw new JwtException("Invalid token type (access required)");
        }
        return claims.getSubject();
    }

    /** ✅ refresh 토큰인지 확인까지 포함 */
    public String getUsernameFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TYPE_REFRESH.equals(typ)) {
            throw new JwtException("Invalid token type (refresh required)");
        }
        return claims.getSubject();
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /** (선택) 단순 boolean이 필요하면 유지해도 됨 */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Key getKey() {
        return key;
    }
}
