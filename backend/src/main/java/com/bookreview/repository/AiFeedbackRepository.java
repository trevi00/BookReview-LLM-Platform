package com.bookreview.repository;

import com.bookreview.domain.AiFeedback;
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
 * AI 피드백 데이터 접근 인터페이스
 */
@Repository
public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    /**
     * 특정 독서 노트의 모든 AI 피드백 조회
     */
    List<AiFeedback> findByReadingNoteIdOrderByCreatedAtDesc(Long readingNoteId);

    /**
     * 특정 독서 노트의 특정 타입 피드백 조회
     */
    List<AiFeedback> findByReadingNoteIdAndFeedbackTypeOrderByCreatedAtDesc(
            Long readingNoteId, com.bookreview.domain.enums.FeedbackType feedbackType);

    /**
     * 특정 사용자의 모든 AI 피드백 조회
     */
    @Query("SELECT af FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "ORDER BY af.createdAt DESC")
    Page<AiFeedback> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 최근 AI 피드백들 조회 (특정 사용자)
     */
    @Query("SELECT af FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.createdAt >= :fromDate " +
           "ORDER BY af.createdAt DESC")
    List<AiFeedback> findRecentFeedbacks(@Param("userId") Long userId,
                                        @Param("fromDate") LocalDateTime fromDate);

    /**
     * 유용함으로 표시된 피드백들 조회
     */
    @Query("SELECT af FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.isUseful = true " +
           "ORDER BY af.createdAt DESC")
    List<AiFeedback> findUsefulFeedbacks(@Param("userId") Long userId);

    /**
     * 높은 평점의 피드백들 조회
     */
    @Query("SELECT af FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.userRating >= :minRating " +
           "ORDER BY af.userRating DESC, af.createdAt DESC")
    List<AiFeedback> findHighRatedFeedbacks(@Param("userId") Long userId,
                                           @Param("minRating") Integer minRating);

    /**
     * AI 모델별 피드백 통계
     */
    @Query("SELECT af.aiModel, COUNT(af), AVG(af.userRating), AVG(af.confidenceScore) " +
           "FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "GROUP BY af.aiModel")
    List<Object[]> getModelStatistics(@Param("userId") Long userId);

    /**
     * 피드백 타입별 통계
     */
    @Query("SELECT af.feedbackType, COUNT(af), AVG(af.userRating) " +
           "FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "GROUP BY af.feedbackType")
    List<Object[]> getFeedbackTypeStatistics(@Param("userId") Long userId);

    /**
     * 월별 피드백 생성 통계 (H2 호환)
     */
    @Query("SELECT EXTRACT(YEAR FROM af.createdAt), EXTRACT(MONTH FROM af.createdAt), COUNT(af) " +
           "FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.createdAt >= :fromDate " +
           "GROUP BY EXTRACT(YEAR FROM af.createdAt), EXTRACT(MONTH FROM af.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM af.createdAt), EXTRACT(MONTH FROM af.createdAt)")
    List<Object[]> getMonthlyFeedbackStats(@Param("userId") Long userId,
                                          @Param("fromDate") LocalDateTime fromDate);

    /**
     * 특정 책에 대한 모든 AI 피드백 조회
     */
    @Query("SELECT af FROM AiFeedback af " +
            "JOIN af.readingNote rn " +
            "JOIN rn.chapter c " +
            "JOIN c.userBook ub " +
            "WHERE ub.book.id = :bookId AND rn.user.id = :userId " +
            "ORDER BY af.createdAt DESC")
    List<AiFeedback> findByBookId(@Param("bookId") Long bookId,
                                  @Param("userId") Long userId);

    /**
     * 고신뢰도 피드백들 조회
     */
    @Query("SELECT af FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.confidenceScore >= :minConfidence " +
           "ORDER BY af.confidenceScore DESC")
    List<AiFeedback> findHighConfidenceFeedbacks(@Param("userId") Long userId,
                                                @Param("minConfidence") Double minConfidence);

    /**
     * 평균 처리 시간 조회
     */
    @Query("SELECT AVG(af.processingTimeMs) FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTime(@Param("userId") Long userId);

    /**
     * 전체 토큰 사용량 조회
     */
    @Query("SELECT SUM(af.tokensUsed) FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.tokensUsed IS NOT NULL")
    Long getTotalTokensUsed(@Param("userId") Long userId);

    /**
     * 특정 기간 내 피드백 수 조회
     */
    @Query("SELECT COUNT(af) FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.createdAt BETWEEN :startDate AND :endDate")
    Long countFeedbacksInPeriod(@Param("userId") Long userId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * 평점이 있는 피드백들의 평균 평점
     */
    @Query("SELECT AVG(af.userRating) FROM AiFeedback af " +
           "JOIN af.readingNote rn " +
           "WHERE rn.user.id = :userId " +
           "AND af.userRating IS NOT NULL")
    Double getAverageRating(@Param("userId") Long userId);

    /**
     * 사용자 ID로 피드백 개수 조회
     */
    @Query("SELECT COUNT(af) FROM AiFeedback af JOIN af.readingNote rn JOIN rn.user u WHERE u.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID와 생성일로 피드백 개수 조회
     */
    @Query("SELECT COUNT(af) FROM AiFeedback af JOIN af.readingNote rn JOIN rn.user u WHERE u.id = :userId AND af.createdAt > :createdAt")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("createdAt") java.time.LocalDateTime createdAt);

    /**
     * 독서 노트 ID로 피드백 개수 조회
     */
    long countByReadingNoteId(Long readingNoteId);

    /**
     * 독서 노트 ID로 피드백 삭제
     */
    void deleteByReadingNoteId(Long readingNoteId);
}