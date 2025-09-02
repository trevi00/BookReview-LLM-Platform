package com.bookreview.repository;

import com.bookreview.domain.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 챕터 데이터 접근 인터페이스
 */
@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * 특정 책의 모든 챕터를 순서대로 조회
     */
    List<Chapter> findByUserBook_BookIdOrderByChapterNumber(Long bookId);

    /**
     * 특정 책의 챕터 수 조회
     */
    long countByUserBook_BookId(Long bookId);

    /**
     * 책과 챕터 번호로 챕터 조회
     */
    Optional<Chapter> findByUserBook_BookIdAndChapterNumber(Long bookId, Integer chapterNumber);

    /**
     * 특정 책의 특정 범위 챕터들 조회
     */
    @Query("SELECT c FROM Chapter c WHERE c.userBook.book.id = :bookId " +
           "AND c.chapterNumber BETWEEN :startChapter AND :endChapter " +
           "ORDER BY c.chapterNumber")
    List<Chapter> findChaptersByRange(@Param("bookId") Long bookId,
                                    @Param("startChapter") Integer startChapter,
                                    @Param("endChapter") Integer endChapter);

    /**
     * 제목으로 챕터 검색 (특정 책 내에서)
     */
    @Query("SELECT c FROM Chapter c WHERE c.userBook.book.id = :bookId " +
           "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
           "ORDER BY c.chapterNumber")
    List<Chapter> findByBookIdAndTitleContainingIgnoreCase(@Param("bookId") Long bookId,
                                                          @Param("title") String title);

    /**
     * 특정 페이지 범위가 포함된 챕터 찾기
     */
    @Query("SELECT c FROM Chapter c WHERE c.userBook.book.id = :bookId " +
           "AND :pageNumber BETWEEN c.startPage AND c.endPage")
    Optional<Chapter> findChapterByPage(@Param("bookId") Long bookId,
                                       @Param("pageNumber") Integer pageNumber);

    /**
     * 독서 노트가 있는 챕터들 조회
     */
    @Query("SELECT DISTINCT c FROM Chapter c " +
           "JOIN c.readingNotes rn " +
           "WHERE c.userBook.book.id = :bookId " +
           "ORDER BY c.chapterNumber")
    List<Chapter> findChaptersWithNotes(@Param("bookId") Long bookId);

    /**
     * 특정 사용자가 노트를 작성한 챕터들 조회
     */
    @Query("SELECT DISTINCT c FROM Chapter c " +
           "JOIN c.readingNotes rn " +
           "WHERE c.userBook.book.id = :bookId AND rn.user.id = :userId " +
           "ORDER BY c.chapterNumber")
    List<Chapter> findChaptersWithNotesByUser(@Param("bookId") Long bookId,
                                             @Param("userId") Long userId);

    /**
     * 챕터별 노트 수 통계
     */
    @Query("SELECT c.id, COUNT(rn) FROM Chapter c " +
           "LEFT JOIN c.readingNotes rn " +
           "WHERE c.userBook.book.id = :bookId " +
           "GROUP BY c.id " +
           "ORDER BY c.chapterNumber")
    List<Object[]> getChapterNoteStatistics(@Param("bookId") Long bookId);

    /**
     * 긴 챕터들 조회 (페이지 수 기준)
     */
    @Query("SELECT c FROM Chapter c WHERE c.userBook.book.id = :bookId " +
           "AND (c.endPage - c.startPage + 1) >= :minPages " +
           "ORDER BY (c.endPage - c.startPage + 1) DESC")
    List<Chapter> findLongChapters(@Param("bookId") Long bookId,
                                  @Param("minPages") Integer minPages);

    /**
     * 다음 챕터 번호 가져오기
     */
    @Query("SELECT COALESCE(MAX(c.chapterNumber), 0) + 1 FROM Chapter c WHERE c.userBook.book.id = :bookId")
    Integer getNextChapterNumber(@Param("bookId") Long bookId);

    /**
     * 챕터 번호 중복 확인
     */
    boolean existsByUserBook_BookIdAndChapterNumber(Long bookId, Integer chapterNumber);

    /**
     * 페이지 범위 겹침 확인
     */
    @Query("SELECT COUNT(c) > 0 FROM Chapter c WHERE c.userBook.book.id = :bookId " +
           "AND c.id != :excludeChapterId " +
           "AND ((c.startPage BETWEEN :startPage AND :endPage) " +
           "OR (c.endPage BETWEEN :startPage AND :endPage) " +
           "OR (:startPage BETWEEN c.startPage AND c.endPage))")
    boolean existsOverlappingPageRange(@Param("bookId") Long bookId,
                                      @Param("excludeChapterId") Long excludeChapterId,
                                      @Param("startPage") Integer startPage,
                                      @Param("endPage") Integer endPage);

    /**
     * UserBook ID로 챕터 조회 (ChapterService용)
     */
    List<Chapter> findByUserBookIdOrderByChapterNumber(Long userBookId);

    /**
     * UserBook ID와 챕터 번호로 조회 (ChapterService용)
     */
    Optional<Chapter> findByUserBookIdAndChapterNumber(Long userBookId, Integer chapterNumber);

    /**
     * 페이지 범위 겹치는 챕터들 조회
     */
    @Query("SELECT c FROM Chapter c WHERE c.userBook.id = :userBookId " +
           "AND ((c.startPage BETWEEN :startPage AND :endPage) " +
           "OR (c.endPage BETWEEN :startPage AND :endPage) " +
           "OR (:startPage BETWEEN c.startPage AND c.endPage))")
    List<Chapter> findOverlappingChapters(@Param("userBookId") Long userBookId,
                                         @Param("startPage") Integer startPage,
                                         @Param("endPage") Integer endPage);

    /**
     * 현재 챕터를 제외하고 페이지 범위 겹치는 챕터들 조회
     */
    @Query("SELECT c FROM Chapter c WHERE c.userBook.id = :userBookId " +
           "AND c.id != :excludeChapterId " +
           "AND ((c.startPage BETWEEN :startPage AND :endPage) " +
           "OR (c.endPage BETWEEN :startPage AND :endPage) " +
           "OR (:startPage BETWEEN c.startPage AND c.endPage))")
    List<Chapter> findOverlappingChaptersExcludingCurrent(@Param("userBookId") Long userBookId,
                                                         @Param("startPage") Integer startPage,
                                                         @Param("endPage") Integer endPage,
                                                         @Param("excludeChapterId") Long excludeChapterId);

    /**
     * UserBook ID로 챕터 조회 (정렬 포함)
     */
    List<Chapter> findByUserBookId(Long userBookId, org.springframework.data.domain.Sort sort);
}