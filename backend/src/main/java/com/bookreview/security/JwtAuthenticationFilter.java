package com.bookreview.security;

import com.bookreview.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_STRING = "Authorization";

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String requestHeader = request.getHeader(HEADER_STRING);
        String username = null;
        String authToken = null;

        if (requestHeader != null && requestHeader.startsWith(TOKEN_PREFIX)) {
            authToken = requestHeader.substring(TOKEN_PREFIX.length());
            try {
                // 토큰이 블랙리스트에 있는지 먼저 확인
                if (tokenBlacklistService.isTokenBlacklisted(authToken)) {
                    logger.warn("Blacklisted token detected");
                    sendErrorResponse(response, "Token has been revoked", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                username = jwtUtil.getUsernameFromToken(authToken);
                Long userId = jwtUtil.getUserIdFromToken(authToken);
                
                // 사용자가 로그아웃한 이후에 발급된 토큰인지 확인
                if (tokenBlacklistService.isTokenIssuedBeforeLogout(userId, jwtUtil.getCreatedDateFromToken(authToken))) {
                    logger.warn("Token was issued before user logout: {}", username);
                    sendErrorResponse(response, "Token has been invalidated", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                
                logger.debug("JWT token found for user: {}", username);
            } catch (JwtException e) {
                logger.error("JWT token parsing failed: {}", e.getMessage());
                sendErrorResponse(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            logger.debug("JWT token not found in request header");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtUtil.validateToken(authToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("JWT authentication successful for user: {}", username);
                } else {
                    logger.warn("JWT token validation failed for user: {}", username);
                    sendErrorResponse(response, "JWT token validation failed", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } catch (Exception e) {
                logger.error("Authentication error: {}", e.getMessage());
                sendErrorResponse(response, "Authentication failed", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 인증이 필요없는 경로들
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/favicon.ico") ||
               path.equals("/error");
    }
}