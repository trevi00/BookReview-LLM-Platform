package com.bookreview.dto.auth;

import com.bookreview.domain.enums.AuthProvider;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // 초 단위
    private UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String username;
        private AuthProvider provider;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
    }
}