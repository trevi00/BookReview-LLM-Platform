package com.bookreview.dto.userbook;

import com.bookreview.domain.enums.BookCategory;
import com.bookreview.domain.enums.ReadingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBookSearchRequest {
    
    // 독서 상태 필터
    private ReadingStatus status;
    
    // 책 정보 필터
    private String bookTitle; // 책 제목 (부분 일치)
    private String bookAuthor; // 책 저자 (부분 일치)
    private BookCategory bookCategory; // 책 카테고리
    
    // 평점 필터
    @Min(value = 1, message = "최소 평점은 1 이상이어야 합니다")
    @Max(value = 5, message = "최소 평점은 5 이하여야 합니다")
    private Integer minRating;
    
    @Min(value = 1, message = "최대 평점은 1 이상이어야 합니다")
    @Max(value = 5, message = "최대 평점은 5 이하여야 합니다")
    private Integer maxRating;
    
    // 날짜 필터
    private LocalDateTime startDateFrom; // 시작일 범위 검색
    private LocalDateTime startDateTo;
    private LocalDateTime endDateFrom; // 완료일 범위 검색
    private LocalDateTime endDateTo;
    
    // 진도 필터
    @Min(value = 0, message = "최소 진도는 0 이상이어야 합니다")
    @Max(value = 100, message = "최소 진도는 100 이하여야 합니다")
    private Integer minProgress;
    
    @Min(value = 0, message = "최대 진도는 0 이상이어야 합니다")
    @Max(value = 100, message = "최대 진도는 100 이하여야 합니다")
    private Integer maxProgress;
    
    // 공개/비공개 필터
    private Boolean isPrivate;
    
    // 정렬 옵션
    private String sortBy = "updatedAt"; // status, startDate, endDate, rating, progress, updatedAt
    private String sortDirection = "DESC"; // ASC, DESC
    
    // 페이징
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page = 0;
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100을 초과할 수 없습니다")
    private Integer size = 20;
    
    // 리뷰가 있는 책만 조회
    private Boolean hasReview;
    
    // 노트가 있는 책만 조회
    private Boolean hasNotes;
}