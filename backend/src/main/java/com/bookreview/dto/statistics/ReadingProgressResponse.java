package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 독서 진행 상황 응답 DTO
 */
@Getter
@Builder
public class ReadingProgressResponse {
    private Integer year;
    private Integer targetBooks;
    private Integer completedBooks;
    private Integer currentlyReading;
    private Integer totalNotes;
    private Double goalProgressPercentage;
    private Long totalBooksReading;
    private Long totalPagesRead;
    private Long totalReadingTimeMinutes;
    private Double averageProgress;
    
    private List<BookProgress> currentlyReadingBooks;
    private List<MonthlyProgress> monthlyProgress;
    
    @Getter
    @Builder
    public static class BookProgress {
        private Long bookId;
        private String title;
        private String author;
        private Integer totalPages;
        private Integer currentPage;
        private Double progressPercentage;
        private Long readingTimeMinutes;
    }
}