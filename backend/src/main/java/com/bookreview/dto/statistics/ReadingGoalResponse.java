package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 독서 목표 응답 DTO
 */
@Getter
@Setter
@Builder
public class ReadingGoalResponse {
    private Long id;
    private Long goalId;
    private Integer year;
    private Integer targetBooks;
    private Integer completedBooks;
    private Integer achievedBooks;
    private Double progressPercentage;
    private String description;
    
    private Double achievementRate;
    private Integer remainingBooks;
    private Integer daysRemaining;
    private Double booksPerDayNeeded;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Boolean isAchieved;
    private Boolean isOnTrack;
}