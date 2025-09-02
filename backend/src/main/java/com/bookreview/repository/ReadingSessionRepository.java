package com.bookreview.repository;

import com.bookreview.domain.ReadingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 독서 세션 데이터 접근 인터페이스
 */
@Repository
public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {

    /**
     * 사용자의 모든 독서 세션 조회 (시간 역순)
     */
    @Query("SELECT rs FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId ORDER BY rs.startTime DESC")
    Page<ReadingSession> findByUserIdOrderByStartTimeDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 UserBook의 모든 독서 세션 조회
     */
    List<ReadingSession> findByUserBookIdOrderByStartTimeDesc(Long userBookId);

    /**
     * 사용자의 활성 독서 세션 조회 (종료되지 않은 세션)
     */
    @Query("SELECT rs FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId AND rs.endTime IS NULL")
    Optional<ReadingSession> findActiveSessionByUserId(@Param("userId") Long userId);

    /**
     * 특정 기간 내 독서 세션들 조회
     */
    @Query("SELECT rs FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.startTime BETWEEN :startDate AND :endDate " +
           "ORDER BY rs.startTime DESC")
    List<ReadingSession> findSessionsInPeriod(@Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 기간 내 총 독서 시간 계산
     */
    @Query("SELECT SUM(rs.readingTimeMinutes) FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.startTime BETWEEN :startDate AND :endDate " +
           "AND rs.readingTimeMinutes IS NOT NULL")
    Long getTotalReadingTimeInPeriod(@Param("userId") Long userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 일별 독서 시간 통계 (H2 호환)
     */
    @Query("SELECT CAST(rs.startTime AS date), SUM(rs.readingTimeMinutes) " +
           "FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.startTime BETWEEN :startDate AND :endDate " +
           "AND rs.readingTimeMinutes IS NOT NULL " +
           "GROUP BY CAST(rs.startTime AS date) " +
           "ORDER BY CAST(rs.startTime AS date)")
    List<Object[]> getDailyReadingStats(@Param("userId") Long userId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 월별 독서 시간 통계 (H2 호환)
     */
    @Query("SELECT EXTRACT(YEAR FROM rs.startTime), EXTRACT(MONTH FROM rs.startTime), SUM(rs.readingTimeMinutes) " +
           "FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.startTime BETWEEN :startDate AND :endDate " +
           "AND rs.readingTimeMinutes IS NOT NULL " +
           "GROUP BY EXTRACT(YEAR FROM rs.startTime), EXTRACT(MONTH FROM rs.startTime) " +
           "ORDER BY EXTRACT(YEAR FROM rs.startTime), EXTRACT(MONTH FROM rs.startTime)")
    List<Object[]> getMonthlyReadingStats(@Param("userId") Long userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * 책별 총 독서 시간 조회
     */
    @Query("SELECT ub.book.id, ub.book.title, SUM(rs.readingTimeMinutes) " +
           "FROM ReadingSession rs JOIN rs.userBook ub " +
           "WHERE ub.user.id = :userId " +
           "AND rs.readingTimeMinutes IS NOT NULL " +
           "GROUP BY ub.book.id, ub.book.title " +
           "ORDER BY SUM(rs.readingTimeMinutes) DESC")
    List<Object[]> getReadingTimeByBook(@Param("userId") Long userId);

    /**
     * 평균 독서 세션 시간 조회
     */
    @Query("SELECT AVG(rs.readingTimeMinutes) FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.readingTimeMinutes IS NOT NULL")
    Double getAverageSessionDuration(@Param("userId") Long userId);


    /**
     * 연속 독서 일수 계산을 위한 독서 날짜들 조회 (H2 호환)
     */
    @Query("SELECT DISTINCT CAST(rs.startTime AS date) FROM ReadingSession rs JOIN rs.userBook ub " +
           "WHERE ub.user.id = :userId " +
           "AND rs.startTime >= :fromDate " +
           "ORDER BY CAST(rs.startTime AS date) DESC")
    List<java.time.LocalDate> getReadingDates(@Param("userId") Long userId,
                                            @Param("fromDate") LocalDateTime fromDate);

    /**
     * 시간대별 독서 패턴 분석 (H2 호환)
     */
    @Query("SELECT EXTRACT(HOUR FROM rs.startTime), COUNT(rs), AVG(rs.readingTimeMinutes) " +
           "FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.readingTimeMinutes IS NOT NULL " +
           "GROUP BY EXTRACT(HOUR FROM rs.startTime) " +
           "ORDER BY EXTRACT(HOUR FROM rs.startTime)")
    List<Object[]> getReadingPatternByHour(@Param("userId") Long userId);


    /**
     * 최근 N일간의 독서 세션 수 조회
     */
    @Query("SELECT COUNT(rs) FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.startTime >= :fromDate")
    Long getSessionCountSinceDate(@Param("userId") Long userId,
                                 @Param("fromDate") LocalDateTime fromDate);

    /**
     * 완료되지 않은 오래된 세션들 조회 (정리용)
     */
    @Query("SELECT rs FROM ReadingSession rs WHERE rs.endTime IS NULL " +
           "AND rs.startTime < :cutoffTime")
    List<ReadingSession> findStaleActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 사용자별 총 독서 세션 수 조회
     */
    @Query("SELECT COUNT(rs) FROM ReadingSession rs JOIN rs.userBook ub WHERE ub.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 특정 책의 평균 독서 세션 시간 조회
     */
    @Query("SELECT AVG(rs.readingTimeMinutes) FROM ReadingSession rs " +
           "WHERE rs.userBook.id = :userBookId " +
           "AND rs.readingTimeMinutes IS NOT NULL")
    Double getAverageSessionDurationForBook(@Param("userBookId") Long userBookId);

    /**
     * 사용자별 총 독서 시간 조회 (분 단위) - Service에서 사용
     */
    @Query("SELECT SUM(rs.readingTimeMinutes) FROM ReadingSession rs " +
           "JOIN rs.userBook ub WHERE ub.user.id = :userId")
    Long sumReadingTimeByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID와 날짜 범위로 독서 시간 조회
     */
    @Query("SELECT SUM(rs.readingTimeMinutes) FROM ReadingSession rs " +
           "JOIN rs.userBook ub WHERE ub.user.id = :userId " +
           "AND rs.sessionDate BETWEEN :startDate AND :endDate")
    Long sumReadingTimeByUserIdAndSessionDateBetween(@Param("userId") Long userId,
                                                   @Param("startDate") java.time.LocalDate startDate,
                                                   @Param("endDate") java.time.LocalDate endDate);

    /**
     * 사용자의 최근 독서일 조회
     */
    @Query("SELECT MAX(rs.sessionDate) FROM ReadingSession rs " +
           "JOIN rs.userBook ub WHERE ub.user.id = :userId")
    java.time.LocalDate findLastReadingDateByUserId(@Param("userId") Long userId);

    /**
     * 사용자와 날짜로 독서 활동 존재 확인
     */
    @Query("SELECT COUNT(rs) > 0 FROM ReadingSession rs " +
           "JOIN rs.userBook ub WHERE ub.user.id = :userId AND rs.sessionDate = :sessionDate")
    boolean existsByUserIdAndSessionDate(@Param("userId") Long userId, @Param("sessionDate") java.time.LocalDate sessionDate);

    /**
     * UserBook ID와 날짜 범위로 세션 조회
     */
    List<ReadingSession> findByUserBookIdAndSessionDateBetweenOrderBySessionDateDesc(Long userBookId, 
                                                                                   java.time.LocalDate startDate, 
                                                                                   java.time.LocalDate endDate);

    /**
     * 사용자 ID와 날짜 범위로 세션 조회
     */
    @Query("SELECT rs FROM ReadingSession rs JOIN rs.userBook ub " +
           "WHERE ub.user.id = :userId AND rs.sessionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY rs.sessionDate DESC")
    List<ReadingSession> findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(@Param("userId") Long userId,
                                                                               @Param("startDate") java.time.LocalDate startDate,
                                                                               @Param("endDate") java.time.LocalDate endDate);
}