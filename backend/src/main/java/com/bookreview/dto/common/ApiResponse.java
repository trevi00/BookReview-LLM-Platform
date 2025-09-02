package com.bookreview.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private Boolean success;
    private T data;
    private String message;
    private String errorCode;
    private List<String> errors;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // 성공 응답 생성 메소드들
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .message(message)
            .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .build();
    }

    // 에러 응답 생성 메소드들
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }

    public static <T> ApiResponse<T> error(List<String> errors) {
        return ApiResponse.<T>builder()
            .success(false)
            .errors(errors)
            .build();
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .errors(errors)
            .build();
    }

    // 에러 코드와 메시지를 함께 받는 메서드들 추가
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .errorCode(errorCode)
            .message(message)
            .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, List<String> errors) {
        return ApiResponse.<T>builder()
            .success(false)
            .errorCode(errorCode)
            .message(message)
            .errors(errors)
            .build();
    }

    // 제네릭 데이터와 함께 에러 코드를 받는 메서드 추가
    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return ApiResponse.<T>builder()
            .success(false)
            .errorCode(errorCode)
            .message(message)
            .data(data)
            .build();
    }
}