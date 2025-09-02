package com.bookreview.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("userSecurityService")
public class UserSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(UserSecurityService.class);

    /**
     * 사용자가 특정 userId의 리소스에 접근할 권한이 있는지 확인
     */
    public boolean hasAccessToUser(Authentication authentication, String userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetailsService.UserPrincipal)) {
            return false;
        }

        CustomUserDetailsService.UserPrincipal userPrincipal = (CustomUserDetailsService.UserPrincipal) principal;
        
        try {
            Long requestedUserId = Long.parseLong(userId);
            Long currentUserId = userPrincipal.getId();
            
            // 자신의 리소스이거나 관리자인 경우
            boolean hasAccess = currentUserId.equals(requestedUserId) || 
                               userPrincipal.getAuthorities().stream()
                                   .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAccess) {
                logger.warn("Access denied: User {} tried to access resources of user {}", 
                    currentUserId, requestedUserId);
            }
            
            return hasAccess;
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid userId format: {}", userId);
            return false;
        }
    }

    /**
     * 사용자가 특정 리소스의 소유자인지 확인
     */
    public boolean isResourceOwner(Authentication authentication, Long resourceOwnerId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetailsService.UserPrincipal)) {
            return false;
        }

        CustomUserDetailsService.UserPrincipal userPrincipal = (CustomUserDetailsService.UserPrincipal) principal;
        return userPrincipal.getId().equals(resourceOwnerId);
    }

    /**
     * 관리자 권한 확인
     */
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}