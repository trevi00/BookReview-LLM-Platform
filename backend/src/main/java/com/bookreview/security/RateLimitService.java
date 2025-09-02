package com.bookreview.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int defaultRequestsPerMinute;

    @Value("${app.rate-limit.requests-per-hour:1000}")
    private int defaultRequestsPerHour;

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 분당 요청 수 제한 확인
     */
    public boolean isAllowedPerMinute(String identifier, int limit) {
        return isAllowed(identifier, "minute", limit, 60);
    }

    /**
     * 시간당 요청 수 제한 확인
     */
    public boolean isAllowedPerHour(String identifier, int limit) {
        return isAllowed(identifier, "hour", limit, 3600);
    }

    /**
     * 기본 분당 제한 확인
     */
    public boolean isAllowedPerMinute(String identifier) {
        return isAllowedPerMinute(identifier, defaultRequestsPerMinute);
    }

    /**
     * 기본 시간당 제한 확인
     */
    public boolean isAllowedPerHour(String identifier) {
        return isAllowedPerHour(identifier, defaultRequestsPerHour);
    }

    /**
     * 특정 리소스에 대한 제한 확인
     */
    public boolean isAllowedForResource(String userId, String resource, int limit, int windowSeconds) {
        String identifier = "user:" + userId + ":resource:" + resource;
        return isAllowed(identifier, "custom", limit, windowSeconds);
    }

    /**
     * AI 피드백 요청 제한 (사용자당 일일 제한)
     */
    public boolean isAllowedAIFeedback(String userId, int dailyLimit) {
        String identifier = "ai:feedback:user:" + userId;
        return isAllowed(identifier, "daily", dailyLimit, 86400); // 24시간
    }

    /**
     * 로그인 시도 제한
     */
    public boolean isAllowedLoginAttempt(String ipAddress, int limit) {
        String identifier = "login:ip:" + ipAddress;
        return isAllowed(identifier, "login", limit, 900); // 15분
    }

    /**
     * 회원가입 제한 (IP당)
     */
    public boolean isAllowedSignup(String ipAddress, int limit) {
        String identifier = "signup:ip:" + ipAddress;
        return isAllowed(identifier, "signup", limit, 3600); // 1시간
    }

    private boolean isAllowed(String identifier, String type, int limit, int windowSeconds) {
        try {
            String key = "rate_limit:" + type + ":" + identifier;
            
            // 현재 카운트 조회
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            if (currentCount >= limit) {
                logger.warn("Rate limit exceeded: identifier={}, type={}, limit={}, current={}", 
                    identifier, type, limit, currentCount);
                return false;
            }
            
            // 카운트 증가
            if (currentCount == 0) {
                // 새로운 윈도우 시작
                redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(windowSeconds));
            } else {
                // 기존 윈도우에 추가
                redisTemplate.opsForValue().increment(key);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for identifier: " + identifier, e);
            // Redis 오류 시 기본적으로 허용 (서비스 가용성 우선)
            return true;
        }
    }

    /**
     * 남은 요청 수 조회
     */
    public int getRemainingRequests(String identifier, String type, int limit) {
        try {
            String key = "rate_limit:" + type + ":" + identifier;
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            
            return Math.max(0, limit - currentCount);
            
        } catch (Exception e) {
            logger.error("Error getting remaining requests for identifier: " + identifier, e);
            return limit; // 오류 시 최대값 반환
        }
    }

    /**
     * 제한 리셋 시간 조회 (초)
     */
    public long getResetTimeSeconds(String identifier, String type) {
        try {
            String key = "rate_limit:" + type + ":" + identifier;
            Long expiry = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            return expiry != null ? expiry : 0;
            
        } catch (Exception e) {
            logger.error("Error getting reset time for identifier: " + identifier, e);
            return 0;
        }
    }

    /**
     * 특정 식별자의 제한 해제
     */
    public void resetLimit(String identifier, String type) {
        try {
            String key = "rate_limit:" + type + ":" + identifier;
            redisTemplate.delete(key);
            
            logger.info("Rate limit reset for identifier: {}, type: {}", identifier, type);
            
        } catch (Exception e) {
            logger.error("Error resetting rate limit for identifier: " + identifier, e);
        }
    }

    /**
     * 제한 상태 정보 조회
     */
    public RateLimitStatus getStatus(String identifier, String type, int limit) {
        int remaining = getRemainingRequests(identifier, type, limit);
        long resetTime = getResetTimeSeconds(identifier, type);
        boolean limited = remaining == 0;
        
        return new RateLimitStatus(limit, remaining, resetTime, limited);
    }

    public static class RateLimitStatus {
        private final int limit;
        private final int remaining;
        private final long resetTimeSeconds;
        private final boolean limited;

        public RateLimitStatus(int limit, int remaining, long resetTimeSeconds, boolean limited) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTimeSeconds = resetTimeSeconds;
            this.limited = limited;
        }

        public int getLimit() { return limit; }
        public int getRemaining() { return remaining; }
        public long getResetTimeSeconds() { return resetTimeSeconds; }
        public boolean isLimited() { return limited; }
    }
}