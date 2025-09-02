package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.bookreview.domain.enums.AuthProvider;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 일반 회원가입과 OAuth 로그인을 모두 지원합니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", length = 255)
    private String password; // OAuth 사용자는 null 가능

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public User(String email, String password, String username, String profileImage, 
                AuthProvider provider, String providerId, Boolean isActive) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.profileImage = profileImage;
        this.provider = provider != null ? provider : AuthProvider.LOCAL;
        this.providerId = providerId;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 사용자 프로필 업데이트
     */
    public void updateProfile(String username, String profileImage) {
        this.username = username;
        this.profileImage = profileImage;
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 계정 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * OAuth 사용자인지 확인
     */
    public boolean isOAuthUser() {
        return provider != AuthProvider.LOCAL;
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

}