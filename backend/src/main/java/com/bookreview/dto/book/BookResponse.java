package com.bookreview.dto.book;

import com.bookreview.domain.enums.BookCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {
    
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private Integer publishedYear;
    private String description;
    private Integer totalPages;
    private BookCategory category;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 추가 통계 정보 (필요시 포함)
    private Long totalReaders; // 총 독자 수
    private Double averageRating; // 평균 평점
    private Long totalReviews; // 총 리뷰 수
}