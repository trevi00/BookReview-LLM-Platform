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
public class CreateUserBookRequest {
    
    @NotNull(message = "책 ID는 필수입니다")
    private Long bookId;
    
    @NotNull(message = "독서 상태는 필수입니다")
    private ReadingStatus status;
    
    private LocalDateTime startDate;
    
    @Min(value = 0, message = "현재 페이지는 0 이상이어야 합니다")
    private Integer currentPage = 0;
    
    @Builder.Default
    private Boolean isPrivate = false; // 기본값: 공개
}