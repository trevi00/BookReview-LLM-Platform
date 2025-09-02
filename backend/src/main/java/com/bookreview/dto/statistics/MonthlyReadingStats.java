package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;

/**
 * 월별 독서 통계 DTO
 */
@Getter
@Builder
public class MonthlyReadingStats {
    private Integer year;
    private Integer month;
    private Integer booksRead;
    private Integer notesWritten;
    private Double readingHours;
    private Long booksCompleted;
    private Long readingTimeMinutes;
    private Long notesCreated;
    private Double averageRating;
    private Long pagesRead;
}