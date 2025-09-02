package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 피드백 엔티티
 * 독서 기록에 대한 AI의 피드백을 저장합니다.
 */
@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_note_id", nullable = false)
    private ReadingNote readingNote;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private FeedbackType feedbackType = FeedbackType.COMMENT;

    @Column(name = "ai_model", nullable = false, length = 50)
    private String aiModel;

    @Column(name = "is_useful")
    private Boolean isUseful; // 사용자 평가

    @Column(name = "user_rating")
    private Integer userRating; // 1-5 점수

    @Builder
    public Feedback(ReadingNote readingNote, String content, FeedbackType feedbackType, String aiModel) {
        this.readingNote = readingNote;
        this.content = content;
        this.feedbackType = feedbackType != null ? feedbackType : FeedbackType.COMMENT;
        this.aiModel = aiModel;
        
        validateContent();
    }

    /**
     * 사용자 피드백 유용성 평가
     */
    public void markAsUseful(boolean useful) {
        this.isUseful = useful;
    }

    /**
     * 사용자 피드백 평점 설정
     */
    public void setUserRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("평점은 1-5 사이의 값이어야 합니다.");
        }
        this.userRating = rating;
    }

    /**
     * 피드백 내용 유효성 검사
     */
    private void validateContent() {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("피드백 내용은 비어있을 수 없습니다.");
        }
    }

    /**
     * 피드백 요약 (처음 200자)
     */
    public String getContentSummary() {
        if (content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200) + "...";
    }

    /**
     * 긍정적 평가인지 확인
     */
    public boolean isPositivelyRated() {
        return isUseful != null && isUseful && 
               userRating != null && userRating >= 4;
    }

    /**
     * 피드백 타입 열거형
     */
    public enum FeedbackType {
        COMMENT("코멘트"),
        QUESTION("질문"),
        SUGGESTION("제안");

        private final String displayName;

        FeedbackType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}