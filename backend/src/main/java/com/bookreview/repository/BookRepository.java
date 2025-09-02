package com.bookreview.repository;

import com.bookreview.domain.Book;
import com.bookreview.domain.enums.BookCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 책 Repository
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    /**
     * ISBN으로 책 조회
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * 제목으로 책 검색 (부분 일치)
     */
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 저자로 책 검색 (부분 일치)
     */
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);

    /**
     * 카테고리별 책 조회
     */
    Page<Book> findByCategory(BookCategory category, Pageable pageable);

    /**
     * 제목과 저자로 책 검색 (복합 검색)
     */
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:category IS NULL OR b.category = :category)")
    Page<Book> findBooksWithFilters(@Param("title") String title,
                                   @Param("author") String author,
                                   @Param("category") BookCategory category,
                                   Pageable pageable);

    /**
     * 전체 텍스트 검색 (제목, 저자, 설명)
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Book> searchBooks(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * ISBN 존재 여부 확인
     */
    boolean existsByIsbn(String isbn);

    /**
     * 출판년도별 책 수 조회
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.publishedYear = :year")
    long countByPublishedYear(@Param("year") Integer year);

    /**
     * 카테고리별 책 수 조회
     */
    @Query("SELECT b.category, COUNT(b) FROM Book b GROUP BY b.category ORDER BY COUNT(b) DESC")
    List<Object[]> countByCategory();

    /**
     * 최근 등록된 책 조회
     */
    @Query("SELECT b FROM Book b ORDER BY b.createdAt DESC")
    Page<Book> findRecentBooks(Pageable pageable);

    /**
     * 인기 책 조회 (UserBook에서 많이 선택된 책)
     */
    @Query("SELECT b FROM Book b " +
           "LEFT JOIN UserBook ub ON b.id = ub.book.id " +
           "GROUP BY b.id " +
           "ORDER BY COUNT(ub) DESC")
    List<Book> findPopularBooks(Pageable pageable);

    /**
     * 제목과 저자로 책 존재 여부 확인
     */
    Optional<Book> findByTitleAndAuthor(String title, String author);

    /**
     * 특정 책을 등록한 사용자 수 조회
     */
    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.book.id = :bookId")
    long countUserBooksByBookId(@Param("bookId") Long bookId);
}