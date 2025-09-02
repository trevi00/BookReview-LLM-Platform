package com.bookreview.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil 단위 테스트")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    
    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-testing-minimum-256-bits-long-secret";
    private static final Long TEST_EXPIRATION = 3600L; // 1시간
    private static final Long TEST_REFRESH_EXPIRATION = 86400L; // 24시간
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USERNAME = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", TEST_REFRESH_EXPIRATION);

        userDetails = User.builder()
            .username(TEST_USERNAME)
            .password("password")
            .authorities(new ArrayList<>())
            .build();
    }

    @Test
    @DisplayName("토큰 생성이 정상적으로 동작한다")
    void generateToken_WithValidParameters_ShouldReturnToken() {
        // When
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 header.payload.signature 형태
    }

    @Test
    @DisplayName("토큰에서 사용자명을 올바르게 추출한다")
    void getUsernameFromToken_WithValidToken_ShouldReturnUsername() {
        // Given
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);

        // When
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("토큰에서 사용자 ID를 올바르게 추출한다")
    void getUserIdFromToken_WithValidToken_ShouldReturnUserId() {
        // Given
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);

        // When
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("토큰에서 만료일을 올바르게 추출한다")
    void getExpirationDateFromToken_WithValidToken_ShouldReturnExpirationDate() {
        // Given
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);
        Date currentDate = new Date();

        // When
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);

        // Then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate).isAfter(currentDate);
        
        // 만료일이 현재 시간 + expiration 시간과 거의 같은지 확인 (1분 오차 허용)
        long expectedExpiration = currentDate.getTime() + (TEST_EXPIRATION * 1000);
        assertThat(Math.abs(expirationDate.getTime() - expectedExpiration)).isLessThan(60000L);
    }

    @Test
    @DisplayName("유효한 토큰의 검증이 성공한다")
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);

        // When
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰의 검증이 실패한다")
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given - 매우 짧은 만료 시간으로 토큰 생성
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L);
        String expiredToken = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);
        
        // 원래 만료 시간으로 복원
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);

        // When
        Boolean isValid = jwtUtil.validateToken(expiredToken, userDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("잘못된 사용자명의 토큰 검증이 실패한다")
    void validateToken_WithDifferentUsername_ShouldReturnFalse() {
        // Given
        String token = jwtUtil.generateToken(TEST_USER_ID, "different@example.com");

        // When
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 생성이 정상적으로 동작한다")
    void generateRefreshToken_WithValidParameters_ShouldReturnToken() {
        // When
        String refreshToken = jwtUtil.generateRefreshToken(TEST_USER_ID, TEST_USERNAME);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
        
        // 리프레시 토큰의 만료일이 액세스 토큰보다 길어야 함
        Date refreshExpirationDate = jwtUtil.getExpirationDateFromToken(refreshToken);
        String accessToken = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);
        Date accessExpirationDate = jwtUtil.getExpirationDateFromToken(accessToken);
        
        assertThat(refreshExpirationDate).isAfter(accessExpirationDate);
    }

    @Test
    @DisplayName("토큰 갱신이 정상적으로 동작한다")
    void canTokenBeRefreshed_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);

        // When
        Boolean canBeRefreshed = jwtUtil.canTokenBeRefreshed(token);

        // Then
        assertThat(canBeRefreshed).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 갱신할 수 없다")
    void canTokenBeRefreshed_WithExpiredToken_ShouldReturnFalse() {
        // Given - 매우 짧은 만료 시간으로 토큰 생성
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L);
        String expiredToken = jwtUtil.generateToken(TEST_USER_ID, TEST_USERNAME);
        
        // 원래 만료 시간으로 복원
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);

        // When
        Boolean canBeRefreshed = jwtUtil.canTokenBeRefreshed(expiredToken);

        // Then
        assertThat(canBeRefreshed).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에 대해 예외가 발생한다")
    void getUsernameFromToken_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.getUsernameFromToken(invalidToken))
            .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("빈 토큰에 대해 예외가 발생한다")
    void validateToken_WithEmptyToken_ShouldThrowException() {
        // Given
        String emptyToken = "";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.validateToken(emptyToken, userDetails))
            .isInstanceOf(Exception.class);
    }
}