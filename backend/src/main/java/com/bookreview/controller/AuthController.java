package com.bookreview.controller;

import com.bookreview.dto.auth.*;
import com.bookreview.dto.common.ApiResponse;
import com.bookreview.security.CustomUserDetailsService;
import com.bookreview.service.AuthService;
import com.bookreview.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            
            logger.info("Login successful for user: {} from IP: {}", 
                loginRequest.getEmail(), getClientIpAddress(request));
                
            return ResponseEntity.ok(ApiResponse.success(authResponse, "로그인에 성공했습니다"));
            
        } catch (Exception e) {
            logger.error("Login failed for user: {} from IP: {}, error: {}", 
                loginRequest.getEmail(), getClientIpAddress(request), e.getMessage());
                
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "새로운 계정을 생성합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 사용자")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) {
        
        try {
            AuthResponse authResponse = authService.register(registerRequest);
            
            logger.info("Registration successful for user: {} from IP: {}", 
                registerRequest.getEmail(), getClientIpAddress(request));
                
            return ResponseEntity.ok(ApiResponse.success(authResponse, "회원가입에 성공했습니다"));
            
        } catch (Exception e) {
            logger.error("Registration failed for user: {} from IP: {}, error: {}", 
                registerRequest.getEmail(), getClientIpAddress(request), e.getMessage());
                
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        
        try {
            AuthResponse authResponse = authService.refreshToken(refreshTokenRequest);
            
            return ResponseEntity.ok(ApiResponse.success(authResponse, "토큰이 갱신되었습니다"));
            
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보", description = "현재 로그인한 사용자의 정보를 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser() {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetailsService.UserPrincipal userPrincipal = 
                (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();
            
            AuthResponse.UserInfo userInfo = authService.getCurrentUser(userPrincipal.getId());
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "사용자 정보를 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get current user: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("사용자 정보를 조회할 수 없습니다"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 종료합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetailsService.UserPrincipal) {
                CustomUserDetailsService.UserPrincipal userPrincipal = 
                    (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();
                
                authService.logout(userPrincipal.getId());
                
                logger.info("User logged out: {}", userPrincipal.getEmail());
            }
            
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다"));
            
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage());
            
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다"));
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "토큰 검증", description = "현재 액세스 토큰의 유효성을 검증합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 유효"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰 무효")
    })
    public ResponseEntity<ApiResponse<Boolean>> validateToken(HttpServletRequest request) {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtUtil.validateToken(token)) {
                return ResponseEntity.ok(ApiResponse.success(true, "유효한 토큰입니다"));
            } else {
                return ResponseEntity.ok(ApiResponse.success(false, "유효하지 않은 토큰입니다"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(false, "토큰 검증 실패"));
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}