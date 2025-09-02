package com.bookreview.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 테스트용 컨트롤러
 * 서버 동작 상태를 확인하기 위한 간단한 엔드포인트들
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "Test", description = "테스트 및 서버 상태 확인 API")
public class TestController {

    /**
     * 기본 테스트 엔드포인트
     */
    @GetMapping
    @Operation(summary = "기본 테스트", description = "서버가 정상 작동하는지 확인하는 기본 테스트 엔드포인트")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "서버가 정상적으로 작동 중")
    })
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "BookReview Backend is running!");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0-dev");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 헬스체크 테스트
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "BookReview Backend");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 간단한 echo 테스트
     */
    @GetMapping("/echo")
    public ResponseEntity<String> echo() {
        return ResponseEntity.ok("Echo: Server is responding correctly at " + LocalDateTime.now());
    }
}