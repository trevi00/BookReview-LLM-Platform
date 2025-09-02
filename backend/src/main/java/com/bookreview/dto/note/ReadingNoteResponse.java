package com.bookreview.dto.note;

import com.bookreview.domain.enums.NoteType;
import com.bookreview.dto.chapter.ChapterResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingNoteResponse {
    
    private Long id;
    private Long chapterId;
    private Long userId;
    private String content;
    private NoteType noteType;
    private Integer pageNumber;
    private Boolean isPrivate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 관련 정보
    private ChapterResponse chapter; // 챕터 정보
    private Integer chapterNumber; // 챕터 번호
    private String chapterTitle; // 챕터 제목
    private String bookTitle; // 책 제목
    private String bookAuthor; // 책 저자
    
    // AI 피드백 정보 (요약)
    private Long totalFeedbacks; // 총 피드백 수
    private Long feedbackCount; // 피드백 카운트 (별칭)
    private Boolean hasFeedbacks; // 피드백이 있는지 여부
    private Boolean hasFeedback; // 피드백 존재 여부 (별칭)
    private List<FeedbackSummary> recentFeedbacks; // 최근 피드백 요약
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackSummary {
        private Long id;
        private String content;
        private String feedbackType;
        private String aiModel;
        private Boolean isUseful;
        private Integer userRating;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}