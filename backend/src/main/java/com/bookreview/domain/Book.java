package com.bookreview.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.bookreview.domain.enums.BookCategory;

/**
 * 책 엔티티
 * 시스템에 등록된 책의 정보를 저장합니다.
 */
@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "author", nullable = false, length = 200)
    private String author;

    @Column(name = "publisher", length = 200)
    private String publisher;

    @Column(name = "isbn", unique = true, length = 20)
    private String isbn;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private BookCategory category = BookCategory.OTHER;

    @Builder
    public Book(String title, String author, String publisher, String isbn, 
                Integer publishedYear, String description, String coverImageUrl, 
                Integer totalPages, BookCategory category) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.totalPages = totalPages;
        this.category = category != null ? category : BookCategory.OTHER;
    }

    /**
     * 책 정보 업데이트
     */
    public void updateBookInfo(String title, String author, String publisher, String isbn,
                              Integer publishedYear, String description, String coverImageUrl,
                              Integer totalPages, BookCategory category) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.totalPages = totalPages;
        this.category = category != null ? category : BookCategory.OTHER;
    }

    /**
     * 제목 업데이트
     */
    public void updateTitle(String title) {
        this.title = title;
    }

    /**
     * 저자 업데이트
     */
    public void updateAuthor(String author) {
        this.author = author;
    }

    /**
     * 출판사 업데이트
     */
    public void updatePublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 커버 이미지 URL 업데이트
     */
    public void updateCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    /**
     * 총 페이지 수 업데이트
     */
    public void updateTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * 카테고리 업데이트
     */
    public void updateCategory(BookCategory category) {
        this.category = category != null ? category : BookCategory.OTHER;
    }

    /**
     * ISBN 업데이트
     */
    public void updateIsbn(String isbn) {
        this.isbn = isbn;
    }

    /**
     * 출간 년도 업데이트
     */
    public void updatePublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

}