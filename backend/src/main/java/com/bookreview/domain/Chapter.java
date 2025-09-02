package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 목차 엔티티
 * 사용자가 등록한 책의 목차 정보를 저장합니다.
 */
@Entity
@Table(name = "chapters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_book_id", nullable = false)
    private UserBook userBook;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "start_page")
    private Integer startPage;

    @Column(name = "end_page")
    private Integer endPage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadingNote> readingNotes = new ArrayList<>();

    @Builder
    public Chapter(UserBook userBook, Integer chapterNumber, String title, 
                   Integer startPage, Integer endPage, String description) {
        this.userBook = userBook;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.startPage = startPage;
        this.endPage = endPage;
        this.description = description;
        
        validatePageRange();
    }

    /**
     * 목차 정보 업데이트
     */
    public void updateChapter(String title, Integer startPage, Integer endPage, String description) {
        this.title = title;
        this.startPage = startPage;
        this.endPage = endPage;
        this.description = description;
        
        validatePageRange();
    }

    /**
     * 페이지 범위 유효성 검사
     */
    private void validatePageRange() {
        if (startPage != null && endPage != null && startPage > endPage) {
            throw new IllegalArgumentException("시작 페이지는 끝 페이지보다 클 수 없습니다.");
        }
        if (startPage != null && startPage < 0) {
            throw new IllegalArgumentException("시작 페이지는 0보다 작을 수 없습니다.");
        }
        if (endPage != null && endPage < 0) {
            throw new IllegalArgumentException("끝 페이지는 0보다 작을 수 없습니다.");
        }
    }

    /**
     * 목차의 페이지 수 계산
     */
    public Integer getPageCount() {
        if (startPage == null || endPage == null) {
            return null;
        }
        return endPage - startPage + 1;
    }

    /**
     * 특정 페이지가 이 목차에 포함되는지 확인
     */
    public boolean containsPage(Integer pageNumber) {
        if (startPage == null || endPage == null || pageNumber == null) {
            return false;
        }
        return pageNumber >= startPage && pageNumber <= endPage;
    }

    /**
     * 챕터 번호 업데이트
     */
    public void updateChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    /**
     * 제목 업데이트
     */
    public void updateTitle(String title) {
        this.title = title;
    }

    /**
     * 시작 페이지 업데이트
     */
    public void updateStartPage(Integer startPage) {
        this.startPage = startPage;
        validatePageRange();
    }

    /**
     * 종료 페이지 업데이트
     */
    public void updateEndPage(Integer endPage) {
        this.endPage = endPage;
        validatePageRange();
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }
}