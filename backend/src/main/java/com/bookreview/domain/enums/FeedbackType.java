package com.bookreview.domain.enums;

/**
 * AI 피드백 타입 열거형
 */
public enum FeedbackType {
    SUMMARY("요약"),
    ANALYSIS("분석"),
    QUESTION("질문"),
    SUGGESTION("제안"),
    CRITIQUE("비평"),
    ENCOURAGEMENT("격려"),
    INSIGHT("통찰"),
    CONNECTION("연결점");

    private final String description;

    FeedbackType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}