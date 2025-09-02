package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 독서 연속 기록 응답 DTO
 */
@Getter
@Builder
public class ReadingStreakResponse {
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate streakStartDate;
    private LocalDate lastReadingDate;
    private Boolean isStreakActive;
    
    private Integer totalReadingDays;
    private Integer readingDaysThisMonth;
    private Integer readingDaysThisYear;
}