package com.bookreview.dto.chapter;

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
public class ChapterResponse {
    
    private Long id;
    private Long userBookId;
    private Integer chapterNumber;
    private String title;
    private Integer startPage;
    private Integer endPage;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 통계 정보
    private Long totalNotes; // 해당 챕터의 총 노트 수
    private Integer totalPages; // 해당 챕터의 총 페이지 수
    private Boolean hasNotes; // 노트가 있는지 여부
}