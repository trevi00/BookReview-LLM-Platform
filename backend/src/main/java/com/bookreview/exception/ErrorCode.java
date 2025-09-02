package com.bookreview.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // Common
    INVALID_INPUT("COMMON001", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_VALUE("COMMON002", "잘못된 타입의 값입니다.", HttpStatus.BAD_REQUEST),
    ENTITY_NOT_FOUND("COMMON003", "엔티티를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("COMMON004", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_JSON_FORMAT("COMMON005", "올바르지 않은 JSON 형식입니다.", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED("COMMON006", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    
    // Authentication & Authorization
    AUTHENTICATION_FAILED("AUTH001", "인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_JWT_TOKEN("AUTH002", "유효하지 않은 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_JWT_TOKEN("AUTH003", "만료된 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED),
    UNSUPPORTED_JWT_TOKEN("AUTH004", "지원하지 않는 JWT 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_JWT_SIGNATURE("AUTH005", "JWT 서명이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    EMPTY_JWT_CLAIMS("AUTH006", "JWT 클레임이 비어있습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("AUTH007", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    BLACKLISTED_TOKEN("AUTH008", "블랙리스트에 등록된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    
    // User
    USER_NOT_FOUND("USER001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER002", "이미 존재하는 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD("USER003", "비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    WEAK_PASSWORD("USER004", "보안이 약한 비밀번호입니다.", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED("USER005", "계정이 잠겨있습니다.", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED("USER006", "비활성화된 계정입니다.", HttpStatus.FORBIDDEN),
    
    // Book
    BOOK_NOT_FOUND("BOOK001", "책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_BOOK("BOOK002", "이미 존재하는 책입니다.", HttpStatus.CONFLICT),
    INVALID_ISBN("BOOK003", "유효하지 않은 ISBN입니다.", HttpStatus.BAD_REQUEST),
    BOOK_ALREADY_EXISTS("BOOK004", "이미 등록된 책입니다.", HttpStatus.CONFLICT),
    BOOK_NOT_AVAILABLE("BOOK005", "현재 이용할 수 없는 책입니다.", HttpStatus.BAD_REQUEST),
    
    // UserBook
    USER_BOOK_NOT_FOUND("USERBOOK001", "사용자 책 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_BOOK_ALREADY_EXISTS("USERBOOK002", "이미 등록된 사용자 책입니다.", HttpStatus.CONFLICT),
    INVALID_READING_STATUS("USERBOOK003", "유효하지 않은 독서 상태입니다.", HttpStatus.BAD_REQUEST),
    INVALID_PROGRESS("USERBOOK004", "유효하지 않은 독서 진행률입니다.", HttpStatus.BAD_REQUEST),
    
    // Reading Note
    NOTE_NOT_FOUND("NOTE001", "독서 노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOTE_ACCESS_DENIED("NOTE002", "독서 노트에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_NOTE_TYPE("NOTE003", "유효하지 않은 노트 타입입니다.", HttpStatus.BAD_REQUEST),
    NOTE_CONTENT_TOO_LONG("NOTE004", "노트 내용이 너무 깁니다.", HttpStatus.BAD_REQUEST),
    
    // Chapter
    CHAPTER_NOT_FOUND("CHAPTER001", "챕터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CHAPTER_ORDER("CHAPTER002", "유효하지 않은 챕터 순서입니다.", HttpStatus.BAD_REQUEST),
    CHAPTER_ALREADY_EXISTS("CHAPTER003", "이미 존재하는 챕터입니다.", HttpStatus.CONFLICT),
    
    // Reading Statistics
    READING_GOAL_NOT_FOUND("STATS001", "독서 목표를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_READING_GOAL("STATS002", "유효하지 않은 독서 목표입니다.", HttpStatus.BAD_REQUEST),
    READING_SESSION_NOT_FOUND("STATS003", "독서 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_READING_SESSION("STATS004", "유효하지 않은 독서 세션입니다.", HttpStatus.BAD_REQUEST),
    
    // AI Feedback
    AI_SERVICE_UNAVAILABLE("AI001", "AI 서비스를 이용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    AI_FEEDBACK_GENERATION_FAILED("AI002", "AI 피드백 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_AI_REQUEST("AI003", "유효하지 않은 AI 요청입니다.", HttpStatus.BAD_REQUEST),
    AI_QUOTA_EXCEEDED("AI004", "AI 서비스 할당량을 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
    
    // File & Upload
    FILE_NOT_FOUND("FILE001", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_FILE_FORMAT("FILE002", "지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("FILE003", "파일 크기가 제한을 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED("FILE004", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Rate Limiting
    RATE_LIMIT_EXCEEDED("RATE001", "요청 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
    
    // External API
    EXTERNAL_API_ERROR("EXT001", "외부 API 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    EXTERNAL_API_TIMEOUT("EXT002", "외부 API 호출 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}