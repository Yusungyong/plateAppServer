// src/main/java/com/plateapp/plate_main/auth/security/JwtProvider.java
package com.plateapp.plate_main.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final Key key;

    @Value("${jwt.access-expire}")
    private long accessExpire;

    @Value("${jwt.refresh-expire}")
    private long refreshExpire;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret is empty");
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpire);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, role)
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

    public Claims parseClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromAccessToken(String token) {
        Claims claims = parseAccessClaims(token);
        return claims.getSubject();
    }

    public String getRoleFromAccessToken(String token) {
        Claims claims = parseAccessClaims(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

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

    private Claims parseAccessClaims(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TYPE_ACCESS.equals(typ)) {
            throw new JwtException("Invalid token type (access required)");
        }
        return claims;
    }
}
