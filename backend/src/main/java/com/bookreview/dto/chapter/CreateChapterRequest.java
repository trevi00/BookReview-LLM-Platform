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
public class CreateChapterRequest {
    
    @NotNull(message = "사용자 책 ID는 필수입니다")
    private Long userBookId;
    
    @NotNull(message = "챕터 번호는 필수입니다")
    @Min(value = 1, message = "챕터 번호는 1 이상이어야 합니다")
    private Integer chapterNumber;
    
    @NotBlank(message = "챕터 제목은 필수입니다")
    @Size(min = 1, max = 200, message = "챕터 제목은 1자 이상 200자 이하여야 합니다")
    private String title;
    
    @NotNull(message = "시작 페이지는 필수입니다")
    @Min(value = 1, message = "시작 페이지는 1 이상이어야 합니다")
    private Integer startPage;
    
    @NotNull(message = "종료 페이지는 필수입니다")
    @Min(value = 1, message = "종료 페이지는 1 이상이어야 합니다")
    private Integer endPage;
    
    @Size(max = 1000, message = "챕터 설명은 1000자를 초과할 수 없습니다")
    private String description;
    
    @AssertTrue(message = "종료 페이지는 시작 페이지보다 커야 합니다")
    public boolean isPageRangeValid() {
        if (startPage == null || endPage == null) {
            return true; // 다른 validation에서 처리
        }
        return endPage >= startPage;
    }
}