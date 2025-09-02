package com.bookreview.dto.note;

import com.bookreview.domain.AiFeedback;
import com.bookreview.domain.enums.FeedbackType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * AI 피드백 응답 DTO
 */
@Getter
@Builder
public class AiFeedbackResponse {
    
    private Long id;
    private Long feedbackId;
    private Long readingNoteId;
    private Long noteId;
    
    private String feedbackContent;
    private FeedbackType feedbackType;
    private String aiModel;
    
    private Double confidenceScore;
    private Integer tokensUsed;
    private Long processingTimeMs;
    
    private Boolean isUseful;
    private Integer userRating;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 관련 노트 정보
    private String noteContent;
    private String noteTitle;
    private String bookTitle;
    private String bookAuthor;
    private String content;
    private Double confidence;
}