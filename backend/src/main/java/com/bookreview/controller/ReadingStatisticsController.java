package com.bookreview.controller;

import com.bookreview.dto.common.ApiResponse;
import com.bookreview.dto.statistics.*;
import com.bookreview.security.CurrentUser;
import com.bookreview.security.UserPrincipal;
import com.bookreview.service.ReadingStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-statistics")
@Tag(name = "독서 통계", description = "독서 통계 및 목표 관리 API")
public class ReadingStatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(ReadingStatisticsController.class);

    @Autowired
    private ReadingStatisticsService readingStatisticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "독서 대시보드", description = "사용자의 독서 대시보드 정보를 조회합니다")
    public ResponseEntity<ApiResponse<ReadingDashboardResponse>> getReadingDashboard(
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingDashboardResponse dashboard = readingStatisticsService.getReadingDashboard(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(dashboard, "독서 대시보드를 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 대시보드 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/monthly")
    @Operation(summary = "월별 독서 통계", description = "월별 독서 통계를 조회합니다")
    public ResponseEntity<ApiResponse<List<MonthlyReadingStats>>> getMonthlyStatistics(
            @RequestParam(required = false, defaultValue = "12") int months,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<MonthlyReadingStats> stats = readingStatisticsService.getMonthlyStatistics(userPrincipal.getId(), months);
            return ResponseEntity.ok(ApiResponse.success(stats, "월별 독서 통계를 조회했습니다"));
        } catch (Exception e) {
            logger.error("월별 독서 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "카테고리별 독서 통계", description = "카테고리별 독서 통계를 조회합니다")
    public ResponseEntity<ApiResponse<List<CategoryReadingStats>>> getCategoryStatistics(
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<CategoryReadingStats> stats = readingStatisticsService.getCategoryStatistics(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(stats, "카테고리별 독서 통계를 조회했습니다"));
        } catch (Exception e) {
            logger.error("카테고리별 독서 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/goals")
    @Operation(summary = "독서 목표 생성", description = "새로운 독서 목표를 생성합니다")
    public ResponseEntity<ApiResponse<ReadingGoalResponse>> createReadingGoal(
            @Valid @RequestBody CreateReadingGoalRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingGoalResponse goal = readingStatisticsService.createReadingGoal(userPrincipal.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(goal, "독서 목표가 생성되었습니다"));
        } catch (Exception e) {
            logger.error("독서 목표 생성 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/goals")
    @Operation(summary = "독서 목표 조회", description = "사용자의 독서 목표 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<ReadingGoalResponse>>> getReadingGoals(
            @RequestParam(required = false) Integer year,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<ReadingGoalResponse> goals = readingStatisticsService.getReadingGoals(userPrincipal.getId(), year);
            return ResponseEntity.ok(ApiResponse.success(goals, "독서 목표를 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 목표 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/goals/{goalId}")
    @Operation(summary = "독서 목표 수정", description = "독서 목표를 수정합니다")
    public ResponseEntity<ApiResponse<ReadingGoalResponse>> updateReadingGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody UpdateReadingGoalRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingGoalResponse goal = readingStatisticsService.updateReadingGoal(userPrincipal.getId(), goalId, request);
            return ResponseEntity.ok(ApiResponse.success(goal, "독서 목표가 수정되었습니다"));
        } catch (Exception e) {
            logger.error("독서 목표 수정 실패: {}", goalId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/goals/{goalId}")
    @Operation(summary = "독서 목표 삭제", description = "독서 목표를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteReadingGoal(
            @PathVariable Long goalId,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            readingStatisticsService.deleteReadingGoal(userPrincipal.getId(), goalId);
            return ResponseEntity.ok(ApiResponse.success(null, "독서 목표가 삭제되었습니다"));
        } catch (Exception e) {
            logger.error("독서 목표 삭제 실패: {}", goalId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/progress")
    @Operation(summary = "독서 진척도", description = "현재 독서 진척도를 조회합니다")
    public ResponseEntity<ApiResponse<ReadingProgressResponse>> getReadingProgress(
            @RequestParam(required = false) Integer year,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingProgressResponse progress = readingStatisticsService.getReadingProgress(userPrincipal.getId(), year);
            return ResponseEntity.ok(ApiResponse.success(progress, "독서 진척도를 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 진척도 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/streaks")
    @Operation(summary = "독서 연속 기록", description = "독서 연속 기록을 조회합니다")
    public ResponseEntity<ApiResponse<ReadingStreakResponse>> getReadingStreak(
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingStreakResponse streak = readingStatisticsService.getReadingStreak(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(streak, "독서 연속 기록을 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 연속 기록 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reading-session")
    @Operation(summary = "독서 세션 기록", description = "독서 세션을 기록합니다")
    public ResponseEntity<ApiResponse<ReadingSessionResponse>> recordReadingSession(
            @Valid @RequestBody CreateReadingSessionRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingSessionResponse session = readingStatisticsService.recordReadingSession(userPrincipal.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(session, "독서 세션이 기록되었습니다"));
        } catch (Exception e) {
            logger.error("독서 세션 기록 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/reading-sessions")
    @Operation(summary = "독서 세션 조회", description = "독서 세션 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<ReadingSessionResponse>>> getReadingSessions(
            @RequestParam(required = false) Long userBookId,
            @RequestParam(required = false, defaultValue = "30") int days,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<ReadingSessionResponse> sessions = readingStatisticsService.getReadingSessions(
                userPrincipal.getId(), userBookId, days);
            return ResponseEntity.ok(ApiResponse.success(sessions, "독서 세션을 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 세션 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}