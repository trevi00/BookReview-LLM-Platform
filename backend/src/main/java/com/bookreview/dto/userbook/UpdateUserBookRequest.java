package com.bookreview.dto.userbook;

import com.bookreview.domain.enums.ReadingStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserBookRequest {
    
    private ReadingStatus status;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    @Min(value = 0, message = "현재 페이지는 0 이상이어야 합니다")
    private Integer currentPage;
    
    @Min(value = 1, message = "평점은 1 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5 이하여야 합니다")
    private Integer rating;
    
    @Size(max = 2000, message = "리뷰는 2000자를 초과할 수 없습니다")
    private String review;
    
    private Boolean isPrivate;
}