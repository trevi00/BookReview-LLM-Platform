package com.bookreview.domain.enums;

/**
 * 독서 상태 열거형
 */
public enum ReadingStatus {
    NOT_STARTED("시작 안함"),
    PLAN_TO_READ("읽을 예정"),
    READING("읽는 중"),
    COMPLETED("완료"),
    PAUSED("중단"),
    DROPPED("포기");

    private final String description;

    ReadingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}