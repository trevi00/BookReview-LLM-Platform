package com.bookreview.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    // 로그인 실패 추적
    private final ConcurrentHashMap<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();

    // 의심스러운 활동 추적
    private final ConcurrentHashMap<String, AtomicInteger> suspiciousActivity = new ConcurrentHashMap<>();

    @Async
    public void logLoginAttempt(String username, String ipAddress, boolean success, String userAgent) {
        if (success) {
            // 성공 시 실패 기록 제거
            loginAttempts.remove(getAttemptKey(username, ipAddress));
            
            auditLogger.info("LOGIN_SUCCESS: username={}, ip={}, userAgent={}", 
                username, ipAddress, userAgent);
        } else {
            // 실패 시 카운트 증가
            String key = getAttemptKey(username, ipAddress);
            AttemptInfo attemptInfo = loginAttempts.computeIfAbsent(key, 
                k -> new AttemptInfo());
            
            attemptInfo.incrementAttempts();
            
            if (attemptInfo.getAttempts() >= maxLoginAttempts) {
                auditLogger.warn("LOGIN_BLOCKED: username={}, ip={}, attempts={}, userAgent={}", 
                    username, ipAddress, attemptInfo.getAttempts(), userAgent);
            } else {
                auditLogger.warn("LOGIN_FAILED: username={}, ip={}, attempts={}, userAgent={}", 
                    username, ipAddress, attemptInfo.getAttempts(), userAgent);
            }
        }
    }

    @Async
    public void logSecurityEvent(String eventType, String username, String ipAddress, 
                                String details, HttpServletRequest request) {
        auditLogger.warn("SECURITY_EVENT: type={}, username={}, ip={}, details={}, userAgent={}", 
            eventType, username, ipAddress, details, 
            request != null ? request.getHeader("User-Agent") : "unknown");
    }

    @Async
    public void logSuspiciousActivity(String ipAddress, String activityType, String details) {
        String key = ipAddress + ":" + activityType;
        AtomicInteger count = suspiciousActivity.computeIfAbsent(key, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        auditLogger.warn("SUSPICIOUS_ACTIVITY: ip={}, type={}, count={}, details={}", 
            ipAddress, activityType, currentCount, details);
        
        // 임계값 초과 시 추가 조치
        if (currentCount >= 10) {
            auditLogger.error("CRITICAL_SUSPICIOUS_ACTIVITY: ip={}, type={}, count={}", 
                ipAddress, activityType, currentCount);
        }
    }

    @Async
    public void logDataAccess(String username, String resourceType, String resourceId, 
                             String action, boolean success) {
        if (success) {
            auditLogger.info("DATA_ACCESS: username={}, resource={}:{}, action={}", 
                username, resourceType, resourceId, action);
        } else {
            auditLogger.warn("DATA_ACCESS_DENIED: username={}, resource={}:{}, action={}", 
                username, resourceType, resourceId, action);
        }
    }

    @Async
    public void logPrivilegeEscalation(String username, String fromRole, String toRole, 
                                      String ipAddress) {
        auditLogger.warn("PRIVILEGE_ESCALATION: username={}, from={}, to={}, ip={}", 
            username, fromRole, toRole, ipAddress);
    }

    @Async
    public void logTokenEvent(String eventType, String username, String tokenId, 
                             String ipAddress, LocalDateTime expiry) {
        auditLogger.info("TOKEN_EVENT: type={}, username={}, tokenId={}, ip={}, expiry={}", 
            eventType, username, tokenId, ipAddress, expiry);
    }

    public boolean isAccountLocked(String username, String ipAddress) {
        String key = getAttemptKey(username, ipAddress);
        AttemptInfo attemptInfo = loginAttempts.get(key);
        
        if (attemptInfo == null) {
            return false;
        }
        
        // 잠금 해제 시간 확인
        if (attemptInfo.isLockoutExpired(lockoutDurationMinutes)) {
            loginAttempts.remove(key);
            return false;
        }
        
        return attemptInfo.getAttempts() >= maxLoginAttempts;
    }

    public void resetLoginAttempts(String username, String ipAddress) {
        loginAttempts.remove(getAttemptKey(username, ipAddress));
    }

    private String getAttemptKey(String username, String ipAddress) {
        return username + ":" + ipAddress;
    }

    private static class AttemptInfo {
        private int attempts = 0;
        private LocalDateTime firstAttempt = LocalDateTime.now();
        private LocalDateTime lastAttempt = LocalDateTime.now();

        public void incrementAttempts() {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
        }

        public int getAttempts() {
            return attempts;
        }

        public boolean isLockoutExpired(int lockoutDurationMinutes) {
            return lastAttempt.plusMinutes(lockoutDurationMinutes).isBefore(LocalDateTime.now());
        }
    }

    // 정기적으로 오래된 기록 정리
    @Async
    public void cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        loginAttempts.entrySet().removeIf(entry -> 
            entry.getValue().lastAttempt.isBefore(cutoff));
        
        // 의심스러운 활동 기록도 정리 (간단한 구현)
        if (suspiciousActivity.size() > 10000) {
            suspiciousActivity.clear();
        }
        
        logger.debug("Cleaned up old security audit records");
    }
}