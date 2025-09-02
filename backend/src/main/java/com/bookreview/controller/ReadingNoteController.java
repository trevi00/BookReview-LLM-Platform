package com.bookreview.controller;

import com.bookreview.dto.common.ApiResponse;
import com.bookreview.dto.common.PageResponse;
import com.bookreview.dto.note.*;
import com.bookreview.security.CurrentUser;
import com.bookreview.security.UserPrincipal;
import com.bookreview.service.ReadingNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-notes")
@Tag(name = "독서 노트", description = "독서 노트 관리 API")
public class ReadingNoteController {

    private static final Logger logger = LoggerFactory.getLogger(ReadingNoteController.class);

    @Autowired
    private ReadingNoteService readingNoteService;

    @PostMapping
    @Operation(summary = "독서 노트 생성", description = "새로운 독서 노트를 생성합니다")
    public ResponseEntity<ApiResponse<ReadingNoteResponse>> createReadingNote(
            @Valid @RequestBody CreateReadingNoteRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingNoteResponse note = readingNoteService.createReadingNote(userPrincipal.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(note, "독서 노트가 생성되었습니다"));
        } catch (Exception e) {
            logger.error("독서 노트 생성 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "독서 노트 조회", description = "독서 노트를 조회합니다")
    public ResponseEntity<ApiResponse<ReadingNoteResponse>> getReadingNote(
            @PathVariable Long noteId,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingNoteResponse note = readingNoteService.getReadingNote(userPrincipal.getId(), noteId);
            return ResponseEntity.ok(ApiResponse.success(note, "독서 노트를 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 노트 조회 실패: {}", noteId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "독서 노트 수정", description = "독서 노트를 수정합니다")
    public ResponseEntity<ApiResponse<ReadingNoteResponse>> updateReadingNote(
            @PathVariable Long noteId,
            @Valid @RequestBody UpdateReadingNoteRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingNoteResponse note = readingNoteService.updateReadingNote(userPrincipal.getId(), noteId, request);
            return ResponseEntity.ok(ApiResponse.success(note, "독서 노트가 수정되었습니다"));
        } catch (Exception e) {
            logger.error("독서 노트 수정 실패: {}", noteId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "독서 노트 삭제", description = "독서 노트를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteReadingNote(
            @PathVariable Long noteId,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            readingNoteService.deleteReadingNote(userPrincipal.getId(), noteId);
            return ResponseEntity.ok(ApiResponse.success(null, "독서 노트가 삭제되었습니다"));
        } catch (Exception e) {
            logger.error("독서 노트 삭제 실패: {}", noteId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/chapter/{chapterId}")
    @Operation(summary = "챕터별 독서 노트 조회", description = "특정 챕터의 모든 독서 노트를 조회합니다")
    public ResponseEntity<ApiResponse<List<ReadingNoteResponse>>> getNotesByChapter(
            @PathVariable Long chapterId,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<ReadingNoteResponse> notes = readingNoteService.getNotesByChapter(userPrincipal.getId(), chapterId);
            return ResponseEntity.ok(ApiResponse.success(notes, "챕터 노트를 조회했습니다"));
        } catch (Exception e) {
            logger.error("챕터 노트 조회 실패: {}", chapterId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/user-book/{userBookId}")
    @Operation(summary = "책별 독서 노트 조회", description = "특정 책의 모든 독서 노트를 조회합니다")
    public ResponseEntity<ApiResponse<List<ReadingNoteResponse>>> getNotesByUserBook(
            @PathVariable Long userBookId,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<ReadingNoteResponse> notes = readingNoteService.getNotesByUserBook(userPrincipal.getId(), userBookId);
            return ResponseEntity.ok(ApiResponse.success(notes, "책 노트를 조회했습니다"));
        } catch (Exception e) {
            logger.error("책 노트 조회 실패: {}", userBookId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/search")
    @Operation(summary = "독서 노트 검색", description = "조건에 따라 독서 노트를 검색합니다")
    public ResponseEntity<ApiResponse<PageResponse<ReadingNoteResponse>>> searchReadingNotes(
            @Valid @RequestBody ReadingNoteSearchRequest searchRequest,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            Page<ReadingNoteResponse> result = readingNoteService.searchReadingNotes(userPrincipal.getId(), searchRequest);
            PageResponse<ReadingNoteResponse> pageResponse = PageResponse.from(result);
            return ResponseEntity.ok(ApiResponse.success(pageResponse, "독서 노트 검색을 완료했습니다"));
        } catch (Exception e) {
            logger.error("독서 노트 검색 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "독서 노트 통계", description = "사용자의 독서 노트 통계를 조회합니다")
    public ResponseEntity<ApiResponse<ReadingNoteStatisticsResponse>> getReadingNoteStatistics(
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            ReadingNoteStatisticsResponse statistics = readingNoteService.getReadingNoteStatistics(userPrincipal.getId());
            return ResponseEntity.ok(ApiResponse.success(statistics, "독서 노트 통계를 조회했습니다"));
        } catch (Exception e) {
            logger.error("독서 노트 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{noteId}/ai-feedback")
    @Operation(summary = "AI 피드백 요청", description = "독서 노트에 대한 AI 피드백을 요청합니다")
    public ResponseEntity<ApiResponse<AiFeedbackResponse>> requestAiFeedback(
            @PathVariable Long noteId,
            @Valid @RequestBody AiFeedbackRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            AiFeedbackResponse feedback = readingNoteService.requestAiFeedback(userPrincipal.getId(), noteId, request);
            return ResponseEntity.ok(ApiResponse.success(feedback, "AI 피드백을 생성했습니다"));
        } catch (Exception e) {
            logger.error("AI 피드백 요청 실패: {}", noteId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{noteId}/ai-feedback")
    @Operation(summary = "AI 피드백 조회", description = "독서 노트의 AI 피드백을 조회합니다")
    public ResponseEntity<ApiResponse<List<AiFeedbackResponse>>> getAiFeedbacks(
            @PathVariable Long noteId,
            @CurrentUser UserPrincipal userPrincipal) {
        try {
            List<AiFeedbackResponse> feedbacks = readingNoteService.getAiFeedbacks(userPrincipal.getId(), noteId);
            return ResponseEntity.ok(ApiResponse.success(feedbacks, "AI 피드백을 조회했습니다"));
        } catch (Exception e) {
            logger.error("AI 피드백 조회 실패: {}", noteId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}