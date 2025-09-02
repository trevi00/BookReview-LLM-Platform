package com.bookreview.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private static final String USER_LOGOUT_PREFIX = "logout:user:";
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 토큰을 블랙리스트에 추가
     * @param token JWT 토큰
     * @param expiration 토큰 만료 시간
     */
    public void blacklistToken(String token, Date expiration) {
        String key = BLACKLIST_PREFIX + token;
        
        // 토큰이 이미 만료된 경우, 블랙리스트에 추가하지 않음
        if (expiration.before(new Date())) {
            log.debug("Token is already expired, skipping blacklist");
            return;
        }
        
        // 토큰 만료 시간까지 Redis에 저장
        Duration ttl = Duration.between(Instant.now(), expiration.toInstant());
        if (ttl.isPositive()) {
            redisTemplate.opsForValue().set(key, "blacklisted", ttl);
            log.debug("Token blacklisted successfully with TTL: {} seconds", ttl.getSeconds());
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token JWT 토큰
     * @return 블랙리스트에 있으면 true, 없으면 false
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Token found in blacklist");
            return true;
        }
        
        return false;
    }
    
    /**
     * 사용자의 모든 토큰을 무효화 (로그아웃 시간 기록)
     * @param userId 사용자 ID
     */
    public void invalidateAllUserTokens(Long userId) {
        String key = USER_LOGOUT_PREFIX + userId;
        String logoutTime = String.valueOf(System.currentTimeMillis());
        
        // 사용자 로그아웃 시간을 1개월간 저장 (리프레시 토큰 만료 기간보다 길게)
        redisTemplate.opsForValue().set(key, logoutTime, Duration.ofDays(31));
        
        log.info("All tokens invalidated for user: {}", userId);
    }
    
    /**
     * 토큰이 사용자 로그아웃 시간 이전에 발급되었는지 확인
     * @param userId 사용자 ID
     * @param tokenIssuedAt 토큰 발급 시간
     * @return 로그아웃 이전 토큰이면 true, 아니면 false
     */
    public boolean isTokenIssuedBeforeLogout(Long userId, Date tokenIssuedAt) {
        String key = USER_LOGOUT_PREFIX + userId;
        String logoutTimeStr = redisTemplate.opsForValue().get(key);
        
        if (logoutTimeStr == null) {
            return false; // 로그아웃 기록이 없으면 유효한 토큰
        }
        
        try {
            long logoutTime = Long.parseLong(logoutTimeStr);
            boolean isInvalidToken = tokenIssuedAt.getTime() < logoutTime;
            
            if (isInvalidToken) {
                log.debug("Token was issued before logout for user: {}", userId);
            }
            
            return isInvalidToken;
        } catch (NumberFormatException e) {
            log.error("Invalid logout time format for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 사용자의 로그아웃 기록 삭제 (새로운 로그인 시)
     * @param userId 사용자 ID
     */
    public void clearUserLogoutRecord(Long userId) {
        String key = USER_LOGOUT_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Logout record cleared for user: {}", userId);
    }
    
    /**
     * 블랙리스트에서 토큰 제거 (관리용, 일반적으로는 TTL로 자동 만료)
     * @param token JWT 토큰
     */
    public void removeTokenFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.debug("Token removed from blacklist");
    }
}