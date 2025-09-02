package com.bookreview.repository;

import com.bookreview.domain.ReadingNote;
import com.bookreview.domain.Chapter;
import com.bookreview.domain.User;
import com.bookreview.domain.enums.NoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 독서 기록 Repository
 */
@Repository
public interface ReadingNoteRepository extends JpaRepository<ReadingNote, Long>, JpaSpecificationExecutor<ReadingNote> {

    /**
     * 목차별 독서 기록 조회
     */
    Page<ReadingNote> findByChapter(Chapter chapter, Pageable pageable);

    /**
     * 사용자별 독서 기록 조회
     */
    Page<ReadingNote> findByUser(User user, Pageable pageable);

    /**
     * 사용자별 공개 독서 기록 조회
     */
    Page<ReadingNote> findByUserAndIsPrivateFalse(User user, Pageable pageable);

    /**
     * 사용자별 타입별 독서 기록 조회
     */
    Page<ReadingNote> findByUserAndNoteType(User user, NoteType noteType, Pageable pageable);

    /**
     * 목차별 타입별 독서 기록 조회
     */
    Page<ReadingNote> findByChapterAndNoteType(Chapter chapter, NoteType noteType, Pageable pageable);

    /**
     * 사용자의 독서 기록 수 조회
     */
    long countByUser(User user);

    /**
     * 목차의 독서 기록 수 조회
     */
    long countByChapter(Chapter chapter);

    /**
     * 사용자의 타입별 독서 기록 수 조회
     */
    @Query("SELECT rn.noteType, COUNT(rn) FROM ReadingNote rn WHERE rn.user = :user GROUP BY rn.noteType")
    List<Object[]> countByUserGroupByNoteType(@Param("user") User user);

    /**
     * 특정 기간 내 작성된 독서 기록 조회
     */
    @Query("SELECT rn FROM ReadingNote rn WHERE rn.user = :user " +
           "AND rn.createdAt >= :startDate AND rn.createdAt <= :endDate " +
           "ORDER BY rn.createdAt DESC")
    List<ReadingNote> findNotesInPeriod(@Param("user") User user,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 내용으로 독서 기록 검색
     */
    @Query("SELECT rn FROM ReadingNote rn WHERE rn.user = :user " +
           "AND LOWER(rn.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ReadingNote> searchNotesByContent(@Param("user") User user,
                                          @Param("searchTerm") String searchTerm,
                                          Pageable pageable);

    /**
     * 사용자의 최근 독서 기록 조회
     */
    @Query("SELECT rn FROM ReadingNote rn WHERE rn.user = :user ORDER BY rn.createdAt DESC")
    Page<ReadingNote> findRecentNotesByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 UserBook의 모든 독서 기록 조회
     */
    @Query("SELECT rn FROM ReadingNote rn " +
           "JOIN rn.chapter c " +
           "JOIN c.userBook ub " +
           "WHERE ub.id = :userBookId " +
           "ORDER BY c.chapterNumber, rn.pageNumber")
    List<ReadingNote> findNotesByUserBook(@Param("userBookId") Long userBookId);

    /**
     * 월별 독서 기록 작성 수 조회 (H2 호환)
     */
    @Query("SELECT EXTRACT(YEAR FROM rn.createdAt), EXTRACT(MONTH FROM rn.createdAt), COUNT(rn) FROM ReadingNote rn " +
           "WHERE rn.user = :user " +
           "AND rn.createdAt >= :startDate " +
           "GROUP BY EXTRACT(YEAR FROM rn.createdAt), EXTRACT(MONTH FROM rn.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM rn.createdAt), EXTRACT(MONTH FROM rn.createdAt)")
    List<Object[]> getMonthlyNotesCount(@Param("user") User user,
                                       @Param("startDate") LocalDateTime startDate);

    /**
     * 피드백이 없는 독서 기록 조회
     */
    @Query("SELECT rn FROM ReadingNote rn " +
           "WHERE rn.user = :user " +
           "AND NOT EXISTS (SELECT f FROM Feedback f WHERE f.readingNote = rn)")
    List<ReadingNote> findNotesWithoutFeedback(@Param("user") User user);

    /**
     * 페이지 번호로 독서 기록 조회
     */
    List<ReadingNote> findByChapterAndPageNumber(Chapter chapter, Integer pageNumber);

    /**
     * 챕터별 노트 개수 조회 (ChapterService용)
     */
    long countByChapterId(Long chapterId);

    /**
     * 사용자 ID와 날짜 범위로 노트 개수 조회
     */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);

    /**
     * 사용자 ID와 날짜 범위로 노트 개수 조회
     */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 사용자 ID와 날짜로 노트 존재 여부 확인
     */
    boolean existsByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 사용자 ID로 노트 개수 조회
     */
    long countByUserId(Long userId);

    /**
     * 사용자 ID와 노트 타입으로 노트 개수 조회
     */
    long countByUserIdAndNoteType(Long userId, NoteType noteType);

    /**
     * 챕터 ID로 노트들 조회 (페이지 번호와 생성일 순)
     */
    @Query("SELECT rn FROM ReadingNote rn WHERE rn.chapter.id = :chapterId ORDER BY rn.pageNumber ASC, rn.createdAt ASC")
    List<ReadingNote> findByChapterIdOrderByPageNumberAscCreatedAtAsc(@Param("chapterId") Long chapterId);

    /**
     * UserBook ID로 노트들 조회
     */
    @Query("SELECT rn FROM ReadingNote rn JOIN rn.chapter c WHERE c.userBook.id = :userBookId")
    List<ReadingNote> findByUserBookId(@Param("userBookId") Long userBookId);

    /**
     * 사용자 ID로 평균 노트 길이 조회
     */
    @Query("SELECT AVG(CAST(FUNCTION('LENGTH', rn.content) AS double)) FROM ReadingNote rn JOIN rn.user u WHERE u.id = :userId")
    Double findAverageNoteLengthByUserId(@Param("userId") Long userId);
}