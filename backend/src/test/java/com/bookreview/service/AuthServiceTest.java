package com.bookreview.service;

import com.bookreview.domain.User;
import com.bookreview.domain.enums.AuthProvider;
import com.bookreview.dto.auth.LoginRequest;
import com.bookreview.dto.auth.RegisterRequest;
import com.bookreview.dto.auth.AuthResponse;
import com.bookreview.repository.UserRepository;
import com.bookreview.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = spy(User.builder()
            .email("test@example.com")
            .password("encodedPassword")
            .username("testuser")
            .provider(AuthProvider.LOCAL)
            .isActive(true)
            .build());
        
        // Mock the getId() method to return a value (lenient to avoid UnnecessaryStubbingException)
        lenient().when(testUser.getId()).thenReturn(1L);

        loginRequest = new LoginRequest("test@example.com", "password123", false);
        
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setConfirmPassword("Password123!");
        registerRequest.setUsername("newuser");
    }

    @Test
    @DisplayName("Should return AuthResponse with JWT token on successful login")
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Given
        com.bookreview.security.CustomUserDetailsService.UserPrincipal userPrincipal = 
            mock(com.bookreview.security.CustomUserDetailsService.UserPrincipal.class);
        when(userPrincipal.getId()).thenReturn(1L);
        when(userPrincipal.getUsername()).thenReturn("test@example.com");
        when(userPrincipal.getEmail()).thenReturn("test@example.com");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyLong(), anyString())).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(anyLong(), anyString())).thenReturn("refreshToken");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(anyLong(), anyString());
        verify(jwtUtil).generateRefreshToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should throw exception on invalid credentials")
    void login_WithInvalidCredentials_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should create new user and return JWT token on successful registration")
    void register_WithValidData_ShouldCreateUserAndReturnAuthResponse() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(Long.class), any(String.class))).thenReturn("accessToken");
        when(jwtUtil.generateRefreshToken(any(Long.class), any(String.class))).thenReturn("refreshToken");

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("이미 사용 중인 이메일입니다");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when registering with existing username")
    void register_WithExistingUsername_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("이미 사용 중인 사용자명입니다");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return user info for valid user ID")
    void getCurrentUser_WithValidUserId_ShouldReturnUserInfo() {
        // Given
        Long userId = 1L;
        when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.of(testUser));

        // When
        AuthResponse.UserInfo userInfo = authService.getCurrentUser(userId);

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getId()).isEqualTo(testUser.getId());
        assertThat(userInfo.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(userInfo.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(userInfo.getProvider()).isEqualTo(testUser.getProvider());
        assertThat(userInfo.getIsActive()).isEqualTo(testUser.getIsActive());

        verify(userRepository).findByIdAndIsActiveTrue(userId);
    }

    @Test
    @DisplayName("Should throw exception for invalid user ID")
    void getCurrentUser_WithInvalidUserId_ShouldThrowException() {
        // Given
        Long userId = 999L;
        when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.getCurrentUser(userId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("사용자를 찾을 수 없습니다");

        verify(userRepository).findByIdAndIsActiveTrue(userId);
    }
}