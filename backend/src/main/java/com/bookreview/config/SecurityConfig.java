package com.bookreview.config;

import com.bookreview.security.CustomUserDetailsService;
import com.bookreview.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.and())
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(referrerPolicy -> referrerPolicy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .cacheControl(cacheControl -> cacheControl.and())
            )
            
            .authorizeHttpRequests(auth -> auth
                // 인증이 필요없는 공개 엔드포인트
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                
                // Actuator 엔드포인트 (모니터링용)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").access((authentication, context) -> isDevEnvironment(context)) // 개발 환경에서만 모든 actuator 허용
                
                // 테스트 엔드포인트
                .requestMatchers("/api/test/**").permitAll()
                
                // API 문서 (프로덕션에서는 제한)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").access((authentication, context) -> isDevEnvironment(context))
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs").access((authentication, context) -> isDevEnvironment(context))
                .requestMatchers("/swagger-resources/**", "/webjars/**").access((authentication, context) -> isDevEnvironment(context))
                .requestMatchers("/favicon.ico", "/error").permitAll()
                
                // 책 관련 읽기 전용 엔드포인트 (일부 공개)
                .requestMatchers(HttpMethod.GET, "/api/books/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books/popular").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books/recent").permitAll()
                
                // 관리자 전용 엔드포인트
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 사용자별 리소스 접근 제한 (임시로 authenticated 사용)
                .requestMatchers(HttpMethod.GET, "/api/users/{userId}/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/{userId}/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/{userId}/**").authenticated()
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 오리진 - 환경별 설정
        if ("prod".equals(activeProfile)) {
            // 프로덕션 환경: 특정 도메인만 허용
            configuration.setAllowedOrigins(Arrays.asList(
                "https://yourdomain.com",
                "https://www.yourdomain.com"
            ));
        } else {
            // 개발/테스트 환경: localhost 허용
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://localhost:*",
                "https://127.0.0.1:*"
            ));
        }
        
        // 허용할 HTTP 메소드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
    
    /**
     * 개발 환경에서만 접근을 허용하는 보안 규칙
     */
    private org.springframework.security.authorization.AuthorizationDecision isDevEnvironment(
            org.springframework.security.web.access.intercept.RequestAuthorizationContext context) {
        boolean isDev = "dev".equals(activeProfile) || "test".equals(activeProfile);
        return new org.springframework.security.authorization.AuthorizationDecision(isDev);
    }
}