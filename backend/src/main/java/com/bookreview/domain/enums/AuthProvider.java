package com.bookreview.domain.enums;

/**
 * 인증 제공자 열거형
 */
public enum AuthProvider {
    LOCAL("로컬"),
    GOOGLE("구글");

    private final String description;

    AuthProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}