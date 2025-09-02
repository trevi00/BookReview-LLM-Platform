package com.bookreview.dto.statistics;

import com.bookreview.domain.enums.NoteType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 독서 노트 통계 응답 DTO
 */
@Getter
@Builder
public class ReadingNoteStatisticsResponse {
    
    private Long totalNotes;
    private Long notesThisWeek;
    private Long notesThisMonth;
    private Long notesThisYear;
    
    private Map<NoteType, Long> noteTypeDistribution;
    private Double averageNoteLength;
    
    private Long totalFeedbacks;
    private Long feedbacksThisWeek;
    private Long feedbacksThisMonth;
    
    private String mostActiveDay;
    private String preferredNoteType;
    
    // 개인 기록
    private Long longestNote;
    private String mostProductiveDay;
    private Integer consecutiveDays;
}