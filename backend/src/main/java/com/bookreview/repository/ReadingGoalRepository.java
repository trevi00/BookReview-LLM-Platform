package com.bookreview.repository;

import com.bookreview.domain.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 독서 목표 데이터 접근 인터페이스
 */
@Repository
public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {

    /**
     * 사용자의 특정 연도 독서 목표 조회
     */
    @Query("SELECT rg FROM ReadingGoal rg WHERE rg.user.id = :userId AND rg.year = :year")
    ReadingGoal findByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * ID와 사용자 ID로 독서 목표 조회
     */
    Optional<ReadingGoal> findByIdAndUserId(Long id, Long userId);

    /**
     * 사용자의 모든 독서 목표 조회 (연도 역순)
     */
    List<ReadingGoal> findByUserIdOrderByYearDesc(Long userId);

    /**
     * 특정 연도의 모든 사용자 독서 목표 조회
     */
    List<ReadingGoal> findByYear(Integer year);

    /**
     * 사용자의 최근 독서 목표 조회
     */
    @Query("SELECT rg FROM ReadingGoal rg WHERE rg.user.id = :userId ORDER BY rg.year DESC")
    Optional<ReadingGoal> findLatestByUserId(@Param("userId") Long userId);

    /**
     * 목표 달성률이 특정 비율 이상인 사용자들의 목표 조회
     */
    @Query("SELECT rg FROM ReadingGoal rg WHERE rg.year = :year " +
           "AND (rg.currentBooks * 100.0 / rg.targetBooks) >= :achievementRate")
    List<ReadingGoal> findGoalsWithAchievementRateAbove(@Param("year") Integer year,
                                                       @Param("achievementRate") Double achievementRate);

    /**
     * 연도별 평균 목표 도서 수 조회
     */
    @Query("SELECT rg.year, AVG(rg.targetBooks) FROM ReadingGoal rg GROUP BY rg.year ORDER BY rg.year")
    List<Object[]> getAverageTargetBooksByYear();

    /**
     * 연도별 평균 달성률 조회
     */
    @Query("SELECT rg.year, AVG(rg.currentBooks * 100.0 / rg.targetBooks) FROM ReadingGoal rg " +
           "WHERE rg.targetBooks > 0 GROUP BY rg.year ORDER BY rg.year")
    List<Object[]> getAverageAchievementRateByYear();

    /**
     * 목표를 초과 달성한 사용자들 조회
     */
    @Query("SELECT rg FROM ReadingGoal rg WHERE rg.year = :year " +
           "AND rg.currentBooks > rg.targetBooks")
    List<ReadingGoal> findOverAchievers(@Param("year") Integer year);

    /**
     * 사용자의 목표 달성 현황 확인
     */
    @Query("SELECT CASE WHEN rg.currentBooks >= rg.targetBooks THEN true ELSE false END " +
           "FROM ReadingGoal rg WHERE rg.user.id = :userId AND rg.year = :year")
    Optional<Boolean> isGoalAchieved(@Param("userId") Long userId, @Param("year") Integer year);

    /**
     * 특정 기간 내 생성된 독서 목표들 조회
     */
    @Query("SELECT rg FROM ReadingGoal rg WHERE rg.createdAt BETWEEN :startDate AND :endDate")
    List<ReadingGoal> findGoalsCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                            @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * 사용자별 총 목표 달성 횟수 조회
     */
    @Query("SELECT rg.user.id, COUNT(rg) FROM ReadingGoal rg " +
           "WHERE rg.currentBooks >= rg.targetBooks " +
           "GROUP BY rg.user.id")
    List<Object[]> getTotalAchievementCountByUser();

}