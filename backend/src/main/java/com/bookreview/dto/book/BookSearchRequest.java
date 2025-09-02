package com.bookreview.dto.book;

import com.bookreview.domain.enums.BookCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSearchRequest {
    
    private String title; // 제목 검색 (부분 일치)
    private String author; // 저자 검색 (부분 일치)
    private String publisher; // 출판사 검색 (부분 일치)
    private String isbn; // ISBN 검색 (정확 일치)
    private BookCategory category; // 카테고리 필터
    
    @Min(value = 1000, message = "시작 년도는 1000년 이후여야 합니다")
    private Integer publishedYearFrom; // 출간년도 범위 검색 시작
    
    @Max(value = 2100, message = "종료 년도는 2100년 이전이어야 합니다")
    private Integer publishedYearTo; // 출간년도 범위 검색 끝
    
    @Min(value = 1, message = "최소 페이지 수는 1페이지 이상이어야 합니다")
    private Integer minPages; // 최소 페이지 수
    
    @Max(value = 10000, message = "최대 페이지 수는 10000페이지를 초과할 수 없습니다")
    private Integer maxPages; // 최대 페이지 수
    
    // 정렬 옵션
    private String sortBy = "createdAt"; // title, author, publishedYear, totalPages, createdAt
    private String sortDirection = "DESC"; // ASC, DESC
    
    // 페이징
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page = 0;
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100을 초과할 수 없습니다")
    private Integer size = 20;
    
    // 전체 텍스트 검색 (제목, 저자, 설명에서 검색)
    private String query;
}