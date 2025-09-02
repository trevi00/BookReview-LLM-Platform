package com.bookreview.domain.enums;

/**
 * 독서 노트 타입 열거형
 */
public enum NoteType {
    MEMO("메모"),
    QUOTE("인용구"),
    QUESTION("질문"),
    REFLECTION("성찰"),
    SUMMARY("요약"),
    ANALYSIS("분석"),
    REVIEW("리뷰"),
    BOOKMARK("북마크"),
    IMPRESSION("감상");

    private final String description;

    NoteType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}