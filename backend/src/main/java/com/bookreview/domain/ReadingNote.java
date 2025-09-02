package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.bookreview.domain.enums.NoteType;

/**
 * 독서 기록 엔티티
 * 사용자가 목차별로 작성한 독서 기록을 저장합니다.
 */
@Entity
@Table(name = "reading_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    private NoteType noteType = NoteType.IMPRESSION;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Builder
    public ReadingNote(Chapter chapter, User user, String content, 
                       NoteType noteType, Integer pageNumber, Boolean isPrivate) {
        this.chapter = chapter;
        this.user = user;
        this.content = content;
        this.noteType = noteType != null ? noteType : NoteType.MEMO;
        this.pageNumber = pageNumber;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        
        validateContent();
        validatePageNumber();
    }

    /**
     * 독서 기록 내용 업데이트
     */
    public void updateContent(String content, NoteType noteType, Integer pageNumber, Boolean isPrivate) {
        this.content = content;
        this.noteType = noteType != null ? noteType : NoteType.MEMO;
        this.pageNumber = pageNumber;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        
        validateContent();
        validatePageNumber();
    }

    /**
     * 공개/비공개 상태 변경
     */
    public void togglePrivacy() {
        this.isPrivate = !this.isPrivate;
    }

    /**
     * 내용 유효성 검사
     */
    private void validateContent() {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("독서 기록 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > 10000) {
            throw new IllegalArgumentException("독서 기록은 10,000자를 초과할 수 없습니다.");
        }
    }

    /**
     * 페이지 번호 유효성 검사
     */
    private void validatePageNumber() {
        if (pageNumber != null) {
            if (pageNumber < 0) {
                throw new IllegalArgumentException("페이지 번호는 0보다 작을 수 없습니다.");
            }
            
            // 목차의 페이지 범위 내에 있는지 확인
            if (!chapter.containsPage(pageNumber)) {
                throw new IllegalArgumentException("페이지 번호가 해당 목차의 범위를 벗어났습니다.");
            }
        }
    }

    /**
     * 내용 요약 (처음 100자)
     */
    public String getContentSummary() {
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100) + "...";
    }

    /**
     * 노트 타입 업데이트
     */
    public void updateNoteType(NoteType noteType) {
        this.noteType = noteType != null ? noteType : NoteType.MEMO;
    }

    /**
     * 페이지 번호 업데이트
     */
    public void updatePageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        validatePageNumber();
    }

    /**
     * 프라이버시 설정 업데이트
     */
    public void updatePrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate != null ? isPrivate : false;
    }

    /**
     * 개별 내용 업데이트
     */
    public void updateContent(String content) {
        this.content = content;
        validateContent();
    }

    /**
     * Private 상태 확인 메서드
     */
    public Boolean isPrivate() {
        return this.isPrivate;
    }

}