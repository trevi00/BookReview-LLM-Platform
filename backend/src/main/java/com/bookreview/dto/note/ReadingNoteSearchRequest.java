package com.bookreview.dto.note;

import com.bookreview.domain.enums.NoteType;
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
public class ReadingNoteSearchRequest {
    
    // 챕터 필터
    private Long chapterId;
    private Long userBookId; // 특정 사용자 책의 모든 노트 검색
    
    // 내용 검색
    private String content; // 노트 내용 (부분 일치)
    
    // 노트 타입 필터
    private NoteType noteType;
    
    // 페이지 번호 필터
    @Min(value = 1, message = "최소 페이지는 1 이상이어야 합니다")
    private Integer minPage;
    
    @Max(value = 10000, message = "최대 페이지는 10000 이하여야 합니다")
    private Integer maxPage;
    
    // 공개/비공개 필터
    private Boolean isPrivate;
    
    // 피드백 존재 여부 필터
    private Boolean hasFeedbacks;
    
    // 날짜 필터
    private LocalDateTime createdDateFrom;
    private LocalDateTime createdDateTo;
    private LocalDateTime updatedDateFrom;
    private LocalDateTime updatedDateTo;
    
    // 책 정보 필터 (조인을 통해)
    private String bookTitle;
    private String bookAuthor;
    
    // 정렬 옵션
    private String sortBy = "createdAt"; // pageNumber, noteType, createdAt, updatedAt
    private String sortDirection = "DESC"; // ASC, DESC
    
    // 페이징
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private Integer page = 0;
    
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100을 초과할 수 없습니다")
    private Integer size = 20;
}