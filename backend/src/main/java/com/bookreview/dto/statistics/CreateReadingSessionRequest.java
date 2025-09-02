package com.bookreview.dto.statistics;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 독서 세션 생성 요청 DTO
 */
@Getter
@Builder
public class CreateReadingSessionRequest {
    
    @NotNull(message = "사용자 책 ID는 필수입니다")
    private Long userBookId;
    
    private LocalDate sessionDate;
    
    @NotNull(message = "독서 시간은 필수입니다")
    @Min(value = 1, message = "독서 시간은 최소 1분이어야 합니다")
    private Integer readingTimeMinutes;
    
    private Integer startPage;
    private Integer endPage;
    
    @Size(max = 1000, message = "메모는 1000자를 초과할 수 없습니다")
    private String notes;
}