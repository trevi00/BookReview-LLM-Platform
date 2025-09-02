package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 독서 세션 엔티티
 * 사용자의 실제 독서 활동을 기록합니다.
 */
@Entity
@Table(name = "reading_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_book_id", nullable = false)
    private UserBook userBook;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "session_date", nullable = false)
    private java.time.LocalDate sessionDate;

    @Column(name = "reading_time_minutes", nullable = false)
    private Integer readingTimeMinutes = 0;

    @Column(name = "start_page")
    private Integer startPage;

    @Column(name = "end_page")
    private Integer endPage;

    @Column(name = "pages_read", nullable = false)
    private Integer pagesRead = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder
    public ReadingSession(UserBook userBook, java.time.LocalDate sessionDate, Integer readingTimeMinutes,
                         Integer startPage, Integer endPage, String notes) {
        this.userBook = userBook;
        this.sessionDate = sessionDate != null ? sessionDate : java.time.LocalDate.now();
        this.startTime = LocalDateTime.now();
        this.readingTimeMinutes = readingTimeMinutes != null ? readingTimeMinutes : 0;
        this.startPage = startPage;
        this.endPage = endPage;
        this.pagesRead = (endPage != null && startPage != null) ? endPage - startPage + 1 : 0;
        this.notes = notes;
    }

    /**
     * 독서 세션 종료
     */
    public void endSession(Integer pagesRead, String notes) {
        this.endTime = LocalDateTime.now();
        this.pagesRead = pagesRead != null ? pagesRead : 0;
        this.notes = notes;
        
        validateSession();
        
        // UserBook의 현재 페이지 업데이트
        if (pagesRead > 0) {
            int newCurrentPage = userBook.getCurrentPage() + pagesRead;
            userBook.updateCurrentPage(newCurrentPage);
        }
    }

    /**
     * 세션 강제 종료 (비정상 종료)
     */
    public void forceEndSession() {
        this.endTime = LocalDateTime.now();
    }

    /**
     * 읽은 페이지 수 업데이트 (세션 진행 중)
     */
    public void updatePagesRead(Integer pagesRead) {
        if (endTime != null) {
            throw new IllegalStateException("종료된 세션의 페이지 수는 변경할 수 없습니다.");
        }
        this.pagesRead = pagesRead != null ? pagesRead : 0;
        validatePagesRead();
    }

    /**
     * 세션 메모 업데이트
     */
    public void updateNotes(String notes) {
        this.notes = notes;
    }

    /**
     * 독서 시간 계산 (분 단위)
     */
    public Long getReadingTimeInMinutes() {
        if (startTime == null) {
            return 0L;
        }
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end).toMinutes();
    }

    /**
     * 독서 시간 계산 (시간 단위)
     */
    public Double getReadingTimeInHours() {
        return getReadingTimeInMinutes() / 60.0;
    }

    /**
     * 분당 읽은 페이지 수 계산
     */
    public Double getPagesPerMinute() {
        Long minutes = getReadingTimeInMinutes();
        if (minutes == 0 || pagesRead == 0) {
            return 0.0;
        }
        return (double) pagesRead / minutes;
    }

    /**
     * 세션이 진행 중인지 확인
     */
    public boolean isActive() {
        return endTime == null;
    }

    /**
     * 세션 유효성 검사
     */
    private void validateSession() {
        if (endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
        validatePagesRead();
    }

    /**
     * 읽은 페이지 수 유효성 검사
     */
    private void validatePagesRead() {
        if (pagesRead < 0) {
            throw new IllegalArgumentException("읽은 페이지 수는 0보다 작을 수 없습니다.");
        }
    }

    /**
     * 세션 요약 정보
     */
    public String getSessionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("독서 시간: ").append(getReadingTimeInMinutes()).append("분");
        summary.append(", 읽은 페이지: ").append(pagesRead).append("페이지");
        if (pagesRead > 0 && getReadingTimeInMinutes() > 0) {
            summary.append(", 분당 ").append(String.format("%.1f", getPagesPerMinute())).append("페이지");
        }
        return summary.toString();
    }
}