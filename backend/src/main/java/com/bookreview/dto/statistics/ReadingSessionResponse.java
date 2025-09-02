package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 독서 세션 응답 DTO
 */
@Getter
@Builder
public class ReadingSessionResponse {
    private Long id;
    private Long sessionId;
    private Long userBookId;
    private String bookTitle;
    private String bookAuthor;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // private Integer durationMinutes; // 중복 필드 제거 - readingTimeMinutes 사용
    
    private Integer startPage;
    private Integer endPage;
    private Integer pagesRead;
    
    private String notes;
    private LocalDate sessionDate;
    private Integer readingTimeMinutes;
    private LocalDateTime createdAt;
}