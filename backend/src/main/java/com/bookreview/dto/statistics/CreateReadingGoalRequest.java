package com.bookreview.dto.statistics;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

/**
 * 독서 목표 생성 요청 DTO
 */
@Getter
@Builder
public class CreateReadingGoalRequest {
    
    @NotNull(message = "연도는 필수입니다")
    @Min(value = 2020, message = "연도는 2020년 이상이어야 합니다")
    @Max(value = 2030, message = "연도는 2030년 이하여야 합니다")
    private Integer year;
    
    @NotNull(message = "목표 도서 수는 필수입니다")
    @Min(value = 1, message = "목표 도서 수는 1권 이상이어야 합니다")
    @Max(value = 1000, message = "목표 도서 수는 1000권 이하여야 합니다")
    private Integer targetBooks;
    
    @Size(max = 500, message = "목표 설명은 500자 이하여야 합니다")
    private String description;
}