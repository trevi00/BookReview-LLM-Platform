package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 독서 대시보드 응답 DTO
 */
@Getter
@Builder
public class ReadingDashboardResponse {
    
    // 기본 통계
    private Long totalBooksRead;
    private Integer booksReadThisYear;
    private Integer booksReadThisMonth; 
    private Integer booksReadThisWeek;
    private Integer currentlyReading;
    private Integer notesThisYear;
    private Integer notesThisMonth;
    private Integer notesThisWeek;
    private Double totalReadingHours;
    private Integer totalFeedbacks;
    private Integer currentStreak;
    private Double goalProgress;
    
    private Long totalReadingTimeMinutes;
    private Long readingTimeThisYear;
    private Long readingTimeThisWeek;
    
    private Long totalNotes;
    
    private Long totalAiFeedbacks;
    private Long aiFeedbacksThisMonth;
    
    // 현재 독서 상태
    private Long currentlyReadingBooks;
    private Double averageReadingProgress;
    
    // 목표 관련
    private ReadingGoalSummary currentYearGoal;
    
    // 최근 활동
    private List<RecentActivityItem> recentActivities;
    
    // 통계 차트 데이터
    private List<MonthlyReadingStats> monthlyStats;
    private List<CategoryStats> categoryStats;
    
    // 연속 독서 일수
    private Integer consecutiveReadingDays;
    
    // 평균값들
    private Double averageRating;
    private Double averageReadingSessionMinutes;
    
    // 개인 기록들
    private PersonalRecords personalRecords;
    
    private LocalDateTime lastUpdated;
    
    @Getter
    @Builder
    public static class ReadingGoalSummary {
        private Long goalId;
        private Integer year;
        private Integer targetBooks;
        private Integer completedBooks;
        private Double achievementRate;
        private Integer remainingBooks;
        private Integer daysRemaining;
        private Double booksPerDayNeeded;
    }
    
    @Getter
    @Builder
    public static class RecentActivityItem {
        private String type; // BOOK_COMPLETED, NOTE_CREATED, GOAL_ACHIEVED 등
        private String description;
        private LocalDateTime timestamp;
        private String bookTitle;
        private String bookAuthor;
    }
    
    @Getter
    @Builder
    public static class MonthlyReadingStats {
        private Integer year;
        private Integer month;
        private Long booksCompleted;
        private Long readingTimeMinutes;
        private Long notesCreated;
        private Double averageRating;
    }
    
    @Getter
    @Builder
    public static class CategoryStats {
        private String category;
        private Long bookCount;
        private Double percentage;
        private Long totalReadingTime;
        private Double averageRating;
    }
    
    @Getter
    @Builder
    public static class PersonalRecords {
        private String longestBookTitle;
        private Integer longestBookPages;
        private String fastestReadBookTitle;
        private Integer fastestReadDays;
        private Integer longestReadingSessionMinutes;
        private Integer maxBooksInMonth;
        private String favoriteAuthor;
        private Integer consecutiveReadingDaysRecord;
    }
}