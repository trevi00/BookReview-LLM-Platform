package com.bookreview.dto.note;

import com.bookreview.domain.enums.NoteType;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReadingNoteRequest {
    
    @NotNull(message = "챕터 ID는 필수입니다")
    private Long chapterId;
    
    @NotBlank(message = "노트 내용은 필수입니다")
    @Size(min = 1, max = 5000, message = "노트 내용은 1자 이상 5000자 이하여야 합니다")
    private String content;
    
    @NotNull(message = "노트 타입은 필수입니다")
    private NoteType noteType;
    
    @NotNull(message = "페이지 번호는 필수입니다")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    private Integer pageNumber;
    
    @Builder.Default
    private Boolean isPrivate = false; // 기본값: 공개
}