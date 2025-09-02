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
public class UpdateReadingNoteRequest {
    
    @Size(min = 1, max = 5000, message = "노트 내용은 1자 이상 5000자 이하여야 합니다")
    private String content;
    
    private NoteType noteType;
    
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    private Integer pageNumber;
    
    private Boolean isPrivate;
}