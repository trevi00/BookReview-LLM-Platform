package com.bookreview.dto.userbook;

import com.bookreview.domain.enums.ReadingStatus;
import com.bookreview.dto.book.BookResponse;
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
public class UserBookResponse {
    
    private Long id;
    private Long userId;
    private Long bookId;
    private ReadingStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;
    
    private Integer currentPage;
    private Integer rating; // 1-5점 평점
    private String review; // 사용자 리뷰
    private Boolean isPrivate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 관련 정보
    private BookResponse book; // 책 정보
    
    // 통계 정보
    private Integer readingProgress; // 읽기 진도율 (%)
    private Long totalReadingDays; // 총 독서 일수
    private Long totalChapters; // 총 챕터 수
    private Long completedChapters; // 완료한 챕터 수
    private Long totalNotes; // 총 노트 수
}