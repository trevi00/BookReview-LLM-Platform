package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 독서 목표 엔티티
 * 사용자의 연간 독서 목표를 관리합니다.
 */
@Entity
@Table(name = "reading_goals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingGoal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "target_books", nullable = false)
    private Integer targetBooks = 0;

    @Column(name = "target_pages", nullable = false)
    private Integer targetPages = 0;

    @Column(name = "current_books", nullable = false)
    private Integer currentBooks = 0;

    @Column(name = "current_pages", nullable = false)
    private Integer currentPages = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder
    public ReadingGoal(User user, Long userId, Integer year, Integer targetBooks, Integer targetPages, String description) {
        this.user = user;
        this.year = year;
        this.targetBooks = targetBooks != null ? targetBooks : 0;
        this.targetPages = targetPages != null ? targetPages : 0;
        this.description = description;
        this.currentBooks = 0;
        this.currentPages = 0;
        
        validateTargets();
    }

    /**
     * 목표 업데이트
     */
    public void updateTargets(Integer targetBooks, Integer targetPages) {
        this.targetBooks = targetBooks != null ? targetBooks : 0;
        this.targetPages = targetPages != null ? targetPages : 0;
        
        validateTargets();
    }

    /**
     * 현재 진행도 업데이트 (시스템에서 자동 계산)
     */
    public void updateProgress(Integer currentBooks, Integer currentPages) {
        this.currentBooks = currentBooks != null ? currentBooks : 0;
        this.currentPages = currentPages != null ? currentPages : 0;
    }

    /**
     * 완료한 책 추가
     */
    public void addCompletedBook(Integer pages) {
        this.currentBooks++;
        if (pages != null && pages > 0) {
            this.currentPages += pages;
        }
    }

    /**
     * 읽은 페이지 추가
     */
    public void addReadPages(Integer pages) {
        if (pages != null && pages > 0) {
            this.currentPages += pages;
        }
    }

    /**
     * 책 목표 달성률 계산
     */
    public double getBooksAchievementRate() {
        if (targetBooks == 0) {
            return 0.0;
        }
        return Math.min((double) currentBooks / targetBooks * 100, 100.0);
    }

    /**
     * 페이지 목표 달성률 계산
     */
    public double getPagesAchievementRate() {
        if (targetPages == 0) {
            return 0.0;
        }
        return Math.min((double) currentPages / targetPages * 100, 100.0);
    }

    /**
     * 전체 목표 달성률 계산 (책과 페이지의 평균)
     */
    public double getOverallAchievementRate() {
        return (getBooksAchievementRate() + getPagesAchievementRate()) / 2;
    }

    /**
     * 목표 달성 여부 확인
     */
    public boolean isGoalAchieved() {
        return currentBooks >= targetBooks && currentPages >= targetPages;
    }

    /**
     * 책 목표까지 남은 수량
     */
    public Integer getRemainingBooks() {
        return Math.max(0, targetBooks - currentBooks);
    }

    /**
     * 페이지 목표까지 남은 수량
     */
    public Integer getRemainingPages() {
        return Math.max(0, targetPages - currentPages);
    }

    /**
     * 목표 책 수 업데이트
     */
    public void updateTargetBooks(Integer targetBooks) {
        this.targetBooks = targetBooks != null ? targetBooks : 0;
        validateTargets();
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 목표 유효성 검사
     */
    private void validateTargets() {
        if (targetBooks < 0) {
            throw new IllegalArgumentException("목표 책 수는 0보다 작을 수 없습니다.");
        }
        if (targetPages < 0) {
            throw new IllegalArgumentException("목표 페이지 수는 0보다 작을 수 없습니다.");
        }
    }
}