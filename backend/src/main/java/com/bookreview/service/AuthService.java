package com.bookreview.service;

import com.bookreview.domain.User;
import com.bookreview.domain.enums.AuthProvider;
import com.bookreview.dto.auth.*;
import com.bookreview.repository.UserRepository;
import com.bookreview.security.CustomUserDetailsService;
import com.bookreview.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private com.bookreview.security.TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration:604800}")
    private Long jwtExpiration;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            CustomUserDetailsService.UserPrincipal userPrincipal = 
                (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();

            // 마지막 로그인 시간 업데이트
            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            user.updateLastLoginAt();
            userRepository.save(user);

            // JWT 토큰 생성
            String accessToken = jwtUtil.generateToken(userPrincipal.getId(), userPrincipal.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(userPrincipal.getId(), userPrincipal.getUsername());

            logger.info("User logged in successfully: {}", userPrincipal.getEmail());

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(AuthResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .provider(user.getProvider())
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .lastLoginAt(user.getLastLoginAt())
                    .build())
                .build();

        } catch (DisabledException e) {
            logger.warn("Login attempt with disabled account: {}", loginRequest.getEmail());
            throw new RuntimeException("계정이 비활성화되었습니다");
        } catch (BadCredentialsException e) {
            logger.warn("Login attempt with invalid credentials: {}", loginRequest.getEmail());
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다");
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}", loginRequest.getEmail(), e);
            throw new RuntimeException("로그인에 실패했습니다");
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        // 사용자명 중복 확인
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("이미 사용 중인 사용자명입니다");
        }

        // 새 사용자 생성
        User user = User.builder()
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .username(registerRequest.getUsername())
            .provider(AuthProvider.LOCAL)
            .isActive(true)
            .build();

        user = userRepository.save(user);

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        logger.info("New user registered: {}", user.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpiration)
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .provider(user.getProvider())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build())
            .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        User user = userRepository.findByEmailAndIsActiveTrue(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 새 액세스 토큰 생성
        String newAccessToken = jwtUtil.generateToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);

        logger.debug("Token refreshed for user: {}", username);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtExpiration)
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .provider(user.getProvider())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build())
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getCurrentUser(Long userId) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return AuthResponse.UserInfo.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .provider(user.getProvider())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }

    public void logout(Long userId) {
        // 사용자의 모든 토큰을 무효화
        tokenBlacklistService.invalidateAllUserTokens(userId);
        logger.info("User logged out and all tokens invalidated: {}", userId);
    }
    
    public void logoutWithToken(String token, Long userId) {
        try {
            // 현재 토큰을 블랙리스트에 추가
            tokenBlacklistService.blacklistToken(token, jwtUtil.getExpirationDateFromToken(token));
            
            // 사용자의 모든 토큰을 무효화
            tokenBlacklistService.invalidateAllUserTokens(userId);
            
            logger.info("User logged out with token blacklisted: {}", userId);
        } catch (Exception e) {
            logger.error("Error during logout for user: {}", userId, e);
            throw new RuntimeException("로그아웃 처리 중 오류가 발생했습니다");
        }
    }
}