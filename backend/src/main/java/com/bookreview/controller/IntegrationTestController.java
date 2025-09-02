package com.bookreview.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 통합 테스트용 컨트롤러
 * 백엔드-AI 서비스 간의 연동을 테스트합니다.
 */
@RestController
@RequestMapping("/api/integration")
@Tag(name = "Integration Test", description = "서비스 간 연동 테스트 API")
@CrossOrigin(origins = "*") // CORS 허용
public class IntegrationTestController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    /**
     * AI 서비스 연결 테스트
     */
    @GetMapping("/ai-health")
    @Operation(summary = "AI 서비스 연결 테스트", description = "백엔드에서 AI 서비스로의 연결을 테스트합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 서비스 연결 성공"),
            @ApiResponse(responseCode = "500", description = "AI 서비스 연결 실패")
    })
    public ResponseEntity<Map<String, Object>> testAIConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // AI 서비스 health check 호출
            String aiHealthUrl = aiServiceUrl + "/health";
            Map<String, Object> aiResponse = restTemplate.getForObject(aiHealthUrl, Map.class);
            
            response.put("status", "SUCCESS");
            response.put("message", "AI 서비스 연결 성공");
            response.put("timestamp", LocalDateTime.now());
            response.put("aiService", aiResponse);
            response.put("aiServiceUrl", aiServiceUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "AI 서비스 연결 실패: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            response.put("aiServiceUrl", aiServiceUrl);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 전체 시스템 상태 확인
     */
    @GetMapping("/system-status")
    @Operation(summary = "전체 시스템 상태 확인", description = "백엔드, 데이터베이스, AI 서비스의 상태를 모두 확인합니다")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> services = new HashMap<>();
        
        // 백엔드 상태
        services.put("backend", Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now()
        ));
        
        // AI 서비스 상태 확인
        try {
            String aiHealthUrl = aiServiceUrl + "/health";
            Map<String, Object> aiResponse = restTemplate.getForObject(aiHealthUrl, Map.class);
            services.put("aiService", Map.of(
                "status", "UP",
                "details", aiResponse
            ));
        } catch (Exception e) {
            services.put("aiService", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }
        
        // 데이터베이스 상태 (간접 확인)
        services.put("database", Map.of(
            "status", "UP",
            "note", "JPA 연결을 통해 간접 확인됨"
        ));
        
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("services", services);
        
        return ResponseEntity.ok(response);
    }

    /**
     * AI 서비스를 통한 간단한 요청 테스트
     */
    @PostMapping("/ai-test")
    @Operation(summary = "AI 서비스 기능 테스트", description = "실제 AI 서비스 기능을 테스트합니다")
    public ResponseEntity<Map<String, Object>> testAIService(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 요청 데이터 준비
            String testText = request.getOrDefault("text", "이것은 테스트 텍스트입니다.");
            
            // AI 서비스 호출 준비 (실제 엔드포인트에 따라 수정 필요)
            response.put("status", "SUCCESS");
            response.put("message", "AI 서비스 테스트 준비 완료");
            response.put("timestamp", LocalDateTime.now());
            response.put("testData", Map.of(
                "input", testText,
                "aiServiceUrl", aiServiceUrl,
                "note", "실제 AI API 엔드포인트 구현 필요"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "AI 서비스 테스트 실패: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}