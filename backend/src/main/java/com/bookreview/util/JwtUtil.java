package com.bookreview.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    private static final String CLAIM_KEY_USERNAME = "sub";
    private static final String CLAIM_KEY_USER_ID = "userId";
    private static final String CLAIM_KEY_AUTHORITIES = "authorities";
    private static final String CLAIM_KEY_CREATED = "created";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800}") // 7 days
    private Long expiration;

    @Value("${jwt.refresh-expiration:2592000}") // 30 days
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        // JWT Secret 키 길이 검증 (최소 32바이트 = 256비트)
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }
        
        if (secret.equals("default-secret-key-change-in-production")) {
            logger.warn("Using default JWT secret key! This is insecure for production use.");
        }
        
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            logger.warn("JWT secret key is shorter than recommended 256 bits (32 bytes). Current length: {} bytes", keyBytes.length);
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Long getUserIdFromToken(String token) {
        final Claims claims = getClaimsFromToken(token);
        return claims.get(CLAIM_KEY_USER_ID, Long.class);
    }

    public Date getCreatedDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.error("JWT parsing error: {}", e.getMessage());
            throw e;
        }
        return claims;
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, userId);
        return generateToken(claims, username);
    }

    public String generateToken(Map<String, Object> claims, String subject) {
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, userId);
        
        final Date createdDate = new Date();
        final Date expirationDate = new Date(createdDate.getTime() + refreshExpiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean canTokenBeRefreshed(String token) {
        try {
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            // Expired tokens cannot be refreshed
            logger.debug("Token is expired and cannot be refreshed: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            // Invalid tokens cannot be refreshed
            logger.error("Invalid token cannot be refreshed: {}", e.getMessage());
            return false;
        }
    }

    public String refreshToken(String token) {
        final Claims claims = getClaimsFromToken(token);
        claims.put(CLAIM_KEY_CREATED, new Date());
        return generateToken(claims, claims.getSubject());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            logger.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}