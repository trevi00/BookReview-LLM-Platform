package com.bookreview.dto.statistics;

import com.bookreview.domain.enums.BookCategory;
import lombok.Builder;
import lombok.Getter;

/**
 * 카테고리별 독서 통계 DTO
 */
@Getter
@Builder
public class CategoryReadingStats {
    private BookCategory category;
    private String categoryName;
    private Integer booksRead;
    private Integer totalPages;
    private Double averageRating;
    private Long bookCount;
    private Double percentage;
    private Long totalReadingTime;
}