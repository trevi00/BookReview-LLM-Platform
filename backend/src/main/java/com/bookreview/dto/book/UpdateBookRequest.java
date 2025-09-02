package com.bookreview.dto.book;

import com.bookreview.domain.enums.BookCategory;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookRequest {
    
    @Size(min = 1, max = 500, message = "책 제목은 1자 이상 500자 이하여야 합니다")
    private String title;
    
    @Size(min = 1, max = 200, message = "저자는 1자 이상 200자 이하여야 합니다")
    private String author;
    
    @Size(max = 200, message = "출판사는 200자를 초과할 수 없습니다")
    private String publisher;
    
    @Pattern(
        regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
        message = "올바른 ISBN 형식이 아닙니다"
    )
    private String isbn;
    
    @Min(value = 1000, message = "출간년도는 1000년 이후여야 합니다")
    @Max(value = 2100, message = "출간년도는 2100년 이전이어야 합니다")
    private Integer publishedYear;
    
    @Size(max = 2000, message = "책 설명은 2000자를 초과할 수 없습니다")
    private String description;
    
    @Min(value = 1, message = "총 페이지 수는 1페이지 이상이어야 합니다")
    @Max(value = 10000, message = "총 페이지 수는 10000페이지를 초과할 수 없습니다")
    private Integer totalPages;
    
    private BookCategory category;
}