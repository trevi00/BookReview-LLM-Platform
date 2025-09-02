package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.*;
import com.bookreview.domain.enums.FeedbackType;

import java.time.LocalDateTime;

/**
 * AI 피드백 엔티티
 * 독서 기록에 대한 AI 생성 피드백을 저장
 */
@Entity
@Table(name = "ai_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AiFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_note_id", nullable = false)
    private ReadingNote readingNote;

    @Column(name = "feedback_content", columnDefinition = "TEXT", nullable = false)
    private String feedbackContent;

    @Column(name = "feedback_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;

    @Column(name = "ai_model", nullable = false)
    private String aiModel;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "is_useful")
    private Boolean isUseful;

    @Column(name = "user_rating")
    private Integer userRating; // 1-5 점

    public void markAsUseful(boolean useful) {
        this.isUseful = useful;
    }

    public void rateFeedback(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.userRating = rating;
    }

    public boolean isHighConfidence() {
        return confidenceScore != null && confidenceScore >= 0.8;
    }

    /**
     * 피드백 내용 getter (별도 메서드)
     */
    public String getContent() {
        return this.feedbackContent;
    }

    /**
     * 신뢰도 getter (별도 메서드)
     */
    public Double getConfidence() {
        return this.confidenceScore;
    }

    /**
     * 추가 Builder 관련 메서드
     */
    public static class AiFeedbackBuilder {
        // 기본 빌더 패턴에 추가 메서드
        public AiFeedbackBuilder content(String content) {
            this.feedbackContent = content;
            return this;
        }
        
        public AiFeedbackBuilder confidence(Double confidence) {
            this.confidenceScore = confidence;
            return this;
        }
    }
}