package com.bookreview.repository;

import com.bookreview.domain.UserBook;
import com.bookreview.domain.User;
import com.bookreview.domain.Book;
import com.bookreview.domain.enums.ReadingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자-책 Repository
 */
@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long>, JpaSpecificationExecutor<UserBook> {

    /**
     * 사용자별 책 목록 조회
     */
    Page<UserBook> findByUser(User user, Pageable pageable);

    /**
     * 사용자별 상태별 책 목록 조회
     */
    Page<UserBook> findByUserAndStatus(User user, ReadingStatus status, Pageable pageable);
    
    /**
     * 사용자 ID로 책 목록 조회
     */
    Page<UserBook> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 사용자 ID와 상태로 책 목록 조회
     */
    List<UserBook> findByUserIdAndStatus(Long userId, ReadingStatus status, Pageable pageable);
    
    /**
     * 사용자 ID와 UserBook ID로 조회
     */
    Optional<UserBook> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 사용자 ID와 책 ID로 조회
     */
    Optional<UserBook> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * 사용자와 책으로 UserBook 조회
     */
    Optional<UserBook> findByUserAndBook(User user, Book book);

    /**
     * 사용자가 특정 책을 등록했는지 확인
     */
    boolean existsByUserAndBook(User user, Book book);

    /**
     * 사용자의 읽고 있는 책 수 조회
     */
    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.user = :user AND ub.status = 'READING'")
    long countReadingBooksByUser(@Param("user") User user);

    /**
     * 사용자의 완료한 책 수 조회
     */
    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.user = :user AND ub.status = 'COMPLETED'")
    long countCompletedBooksByUser(@Param("user") User user);

    /**
     * 사용자의 총 등록 책 수 조회
     */
    long countByUser(User user);
    
    /**
     * 사용자 ID로 책 수 조회
     */
    long countByUserId(Long userId);

    /**
     * 사용자의 평균 평점 조회
     */
    @Query("SELECT AVG(ub.personalRating) FROM UserBook ub WHERE ub.user = :user AND ub.personalRating IS NOT NULL")
    Double getAverageRatingByUser(@Param("user") User user);

    /**
     * 특정 기간 내 완료한 책 조회
     */
    @Query("SELECT ub FROM UserBook ub WHERE ub.user = :user AND ub.status = 'COMPLETED' " +
           "AND ub.endDate >= :startDate AND ub.endDate <= :endDate")
    List<UserBook> findCompletedBooksInPeriod(@Param("user") User user,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 월별 완료한 책 수 조회 (H2 호환)
     */
    @Query("SELECT EXTRACT(YEAR FROM ub.endDate), EXTRACT(MONTH FROM ub.endDate), COUNT(ub) FROM UserBook ub " +
           "WHERE ub.user = :user AND ub.status = 'COMPLETED' " +
           "AND ub.endDate >= :startDate " +
           "GROUP BY EXTRACT(YEAR FROM ub.endDate), EXTRACT(MONTH FROM ub.endDate) " +
           "ORDER BY EXTRACT(YEAR FROM ub.endDate), EXTRACT(MONTH FROM ub.endDate)")
    List<Object[]> getMonthlyCompletedBooksCount(@Param("user") User user,
                                                @Param("startDate") LocalDate startDate);

    /**
     * 사용자의 카테고리별 독서 통계
     */
    @Query("SELECT b.category, COUNT(ub) FROM UserBook ub " +
           "JOIN ub.book b " +
           "WHERE ub.user = :user AND ub.status = 'COMPLETED' " +
           "GROUP BY b.category " +
           "ORDER BY COUNT(ub) DESC")
    List<Object[]> getCategoryStatsByUser(@Param("user") User user);

    /**
     * 연체 중인 책 조회 (30일 이상 진행 중)
     */
    @Query("SELECT ub FROM UserBook ub WHERE ub.user = :user AND ub.status = 'READING' " +
           "AND ub.startDate < :cutoffDate")
    List<UserBook> findOverdueBooks(@Param("user") User user, @Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 최근 활동한 책 조회
     */
    @Query("SELECT ub FROM UserBook ub WHERE ub.user = :user " +
           "ORDER BY ub.updatedAt DESC")
    Page<UserBook> findRecentActivityBooks(@Param("user") User user, Pageable pageable);

    /**
     * ReadingStatisticsService용 메서드들
     */
    long countByUserIdAndStatusAndCompletedAtAfter(Long userId, 
                                                  ReadingStatus status,
                                                  java.time.LocalDateTime completedAt);

    long countByUserIdAndStatusAndStartDateAfter(Long userId,
                                               ReadingStatus status,
                                               java.time.LocalDateTime startDate);

    @Query("SELECT AVG(ub.personalRating) FROM UserBook ub WHERE ub.user.id = :userId " +
            "AND ub.personalRating IS NOT NULL")
    Double getAverageRatingByUserId(@Param("userId") Long userId);

    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId " +
           "AND ub.status = :status " +
           "AND ub.completedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ub.completedAt DESC")
    List<UserBook> findCompletedBooksInPeriod(@Param("userId") Long userId,
                                             @Param("status") ReadingStatus status,
                                             @Param("startDate") java.time.LocalDateTime startDate,
                                             @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 사용자 ID와 상태 및 완료일 범위로 책 개수 조회
     */
    long countByUserIdAndStatusAndCompletedAtBetween(Long userId, ReadingStatus status, 
                                                   java.time.LocalDateTime startDate, 
                                                   java.time.LocalDateTime endDate);

    /**
     * 사용자 ID와 상태로 책 개수 조회
     */
    long countByUserIdAndStatus(Long userId, ReadingStatus status);

    /**
     * 사용자별 카테고리 통계 (ID 기반)
     */
    @Query("SELECT b.category, COUNT(ub), SUM(b.totalPages), AVG(ub.personalRating) FROM UserBook ub " +
           "JOIN ub.book b " +
           "WHERE ub.user.id = :userId AND ub.status = 'COMPLETED' " +
           "GROUP BY b.category " +
           "ORDER BY COUNT(ub) DESC")
    List<Object[]> findCategoryStatsByUserId(@Param("userId") Long userId);
}