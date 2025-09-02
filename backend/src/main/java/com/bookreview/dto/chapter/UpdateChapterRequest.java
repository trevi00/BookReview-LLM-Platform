package com.bookreview.dto.chapter;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateChapterRequest {
    
    @Min(value = 1, message = "챕터 번호는 1 이상이어야 합니다")
    private Integer chapterNumber;
    
    @Size(min = 1, max = 200, message = "챕터 제목은 1자 이상 200자 이하여야 합니다")
    private String title;
    
    @Min(value = 1, message = "시작 페이지는 1 이상이어야 합니다")
    private Integer startPage;
    
    @Min(value = 1, message = "종료 페이지는 1 이상이어야 합니다")
    private Integer endPage;
    
    @Size(max = 1000, message = "챕터 설명은 1000자를 초과할 수 없습니다")
    private String description;
    
    @AssertTrue(message = "종료 페이지는 시작 페이지보다 커야 합니다")
    public boolean isPageRangeValid() {
        if (startPage == null || endPage == null) {
            return true; // 둘 다 null이거나 하나만 null인 경우는 유효하다고 봄
        }
        return endPage >= startPage;
    }
}