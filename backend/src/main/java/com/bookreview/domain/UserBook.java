package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.bookreview.domain.enums.ReadingStatus;


/**
 * 사용자-책 연결 엔티티
 * 사용자가 읽고 있는 책의 상태와 진행 정보를 관리합니다.
 */
@Entity
@Table(name = "user_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReadingStatus status = ReadingStatus.NOT_STARTED;

    @Column(name = "start_date")
    private java.time.LocalDateTime startDate;

    @Column(name = "end_date")
    private java.time.LocalDateTime endDate;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage = 0;

    @Column(name = "personal_rating")
    private Integer personalRating; // 1-5 점수

    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;

    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Builder
    public UserBook(User user, Book book, ReadingStatus status, java.time.LocalDateTime startDate, 
                   Integer currentPage, Boolean isPrivate) {
        this.user = user;
        this.book = book;
        this.status = status != null ? status : ReadingStatus.PLAN_TO_READ;
        this.startDate = startDate;
        this.currentPage = currentPage != null ? currentPage : 0;
        this.isPrivate = isPrivate != null ? isPrivate : false;
    }

    /**
     * 독서 시작
     */
    public void startReading() {
        this.status = ReadingStatus.READING;
        this.startDate = java.time.LocalDateTime.now();
    }

    /**
     * 독서 완료
     */
    public void completeReading() {
        this.status = ReadingStatus.COMPLETED;
        this.endDate = java.time.LocalDateTime.now();
        this.completedAt = java.time.LocalDateTime.now();
        if (this.book.getTotalPages() != null) {
            this.currentPage = this.book.getTotalPages();
        }
    }

    /**
     * 독서 일시정지
     */
    public void pauseReading() {
        this.status = ReadingStatus.PAUSED;
    }

    /**
     * 독서 재개
     */
    public void resumeReading() {
        this.status = ReadingStatus.READING;
    }

    /**
     * 현재 페이지 업데이트
     */
    public void updateCurrentPage(Integer currentPage) {
        if (currentPage < 0) {
            throw new IllegalArgumentException("현재 페이지는 0보다 작을 수 없습니다.");
        }
        this.currentPage = currentPage;
        
        // 총 페이지와 같아지면 자동으로 완료 처리
        if (this.book.getTotalPages() != null && 
            currentPage.equals(this.book.getTotalPages()) && 
            this.status != ReadingStatus.COMPLETED) {
            completeReading();
        }
    }

    /**
     * 개인 평점 설정 (완료된 책만 가능)
     */
    public void setPersonalRating(Integer rating) {
        if (this.status != ReadingStatus.COMPLETED) {
            throw new IllegalStateException("완료된 책만 평점을 설정할 수 있습니다.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1-5 사이의 값이어야 합니다.");
        }
        this.personalRating = rating;
    }

    /**
     * 독서 진행률 계산
     */
    public double getProgressPercentage() {
        if (book.getTotalPages() == null || book.getTotalPages() == 0) {
            return 0.0;
        }
        return (double) currentPage / book.getTotalPages() * 100;
    }

    /**
     * 시작 날짜 업데이트
     */
    public void updateStartDate(java.time.LocalDateTime startDate) {
        this.startDate = startDate;
    }

    /**
     * 종료 날짜 업데이트
     */
    public void updateEndDate(java.time.LocalDateTime endDate) {
        this.endDate = endDate;
    }

    /**
     * 상태 업데이트
     */
    public void updateStatus(ReadingStatus status) {
        this.status = status != null ? status : ReadingStatus.NOT_STARTED;
    }

    /**
     * 평점 필드에 대한 getter (별도 메서드)
     */
    public Integer getRating() {
        return this.personalRating;
    }

    /**
     * 평점 업데이트
     */
    public void updateRating(Integer rating) {
        this.personalRating = rating;
    }

    /**
     * 리뷰 업데이트
     */
    public void updateReview(String review) {
        this.review = review;
    }

    /**
     * 공개/비공개 설정 업데이트
     */
    public void updateIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate != null ? isPrivate : false;
    }

}