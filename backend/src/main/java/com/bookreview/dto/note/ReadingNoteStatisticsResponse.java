package com.bookreview.dto.note;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.bookreview.domain.enums.NoteType;

/**
 * 독서 노트 통계 응답 DTO
 */
@Getter
@Builder
public class ReadingNoteStatisticsResponse {
    
    // 기본 통계
    private Long totalNotes;
    private Long notesThisMonth;
    private Long notesThisWeek;
    private Long notesToday;
    
    // 타입별 통계
    private Map<String, Long> notesByType;
    private String mostUsedType;
    
    // 시간별 통계
    private List<MonthlyNoteStats> monthlyStats;
    private List<DailyNoteStats> dailyStats;
    
    // 책별 통계
    private List<BookNoteStats> topBooksWithNotes;
    
    // 평균값들
    private Double averageNotesPerBook;
    private Double averageNoteLength;
    private Long totalFeedbacks;
    private Long feedbacksThisWeek;
    
    // 최근 활동
    private LocalDateTime lastNoteCreated;
    private Integer consecutiveDaysWithNotes;
    
    @Getter
    @Builder
    public static class MonthlyNoteStats {
        private Integer year;
        private Integer month;
        private Long noteCount;
        private Double averageLength;
    }
    
    @Getter
    @Builder
    public static class DailyNoteStats {
        private String date;
        private Long noteCount;
        private Long totalLength;
    }
    
    @Getter
    @Builder
    public static class BookNoteStats {
        private Long bookId;
        private String bookTitle;
        private String bookAuthor;
        private Long noteCount;
        private LocalDateTime lastNoteDate;
    }
}