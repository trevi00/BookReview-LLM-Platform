package com.bookreview.dto.note;

import com.bookreview.domain.enums.FeedbackType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * AI 피드백 요청 DTO
 */
@Getter
@Builder
public class AiFeedbackRequest {
    
    @NotNull(message = "피드백 타입은 필수입니다")
    private FeedbackType feedbackType;
    
    private String additionalContext; // 추가 컨텍스트
    private String specificQuestion;  // 구체적인 질문
    private String focusArea;        // 집중할 영역 (문체, 내용, 구조 등)
    
    private Boolean includeBookContext; // 책 정보 포함 여부
    private Boolean includeOtherNotes;  // 다른 노트들 참고 여부
    
    // AI 모델 설정
    private String aiModel;          // 사용할 AI 모델 (gpt-4, gpt-3.5-turbo 등)
    private Double temperature;      // 창의성 정도 (0.0 ~ 1.0)
    private Integer maxTokens;       // 최대 토큰 수
}