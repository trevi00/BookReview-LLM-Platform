package com.bookreview.service;

import com.bookreview.domain.Book;
import com.bookreview.domain.enums.BookCategory;
import com.bookreview.dto.book.*;
import com.bookreview.dto.common.PagedResponse;
import com.bookreview.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    @Autowired
    private BookRepository bookRepository;

    public BookResponse createBook(CreateBookRequest request) {
        // ISBN 중복 확인
        if (request.getIsbn() != null && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new RuntimeException("이미 등록된 ISBN입니다: " + request.getIsbn());
        }

        // 동일한 제목과 저자의 책이 있는지 확인
        Optional<Book> existingBook = bookRepository.findByTitleAndAuthor(
            request.getTitle(), request.getAuthor());
        if (existingBook.isPresent()) {
            throw new RuntimeException("동일한 제목과 저자의 책이 이미 존재합니다");
        }

        Book book = Book.builder()
            .title(request.getTitle())
            .author(request.getAuthor())
            .publisher(request.getPublisher())
            .isbn(request.getIsbn())
            .publishedYear(request.getPublishedYear())
            .description(request.getDescription())
            .totalPages(request.getTotalPages())
            .category(request.getCategory())
            .build();

        book = bookRepository.save(book);
        
        logger.info("New book created: {} by {}", book.getTitle(), book.getAuthor());
        
        return convertToResponse(book);
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다: " + bookId));
        
        return convertToResponse(book);
    }

    public BookResponse updateBook(Long bookId, UpdateBookRequest request) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다: " + bookId));

        // ISBN 중복 확인 (현재 책 제외)
        if (request.getIsbn() != null && !request.getIsbn().equals(book.getIsbn()) &&
            bookRepository.existsByIsbn(request.getIsbn())) {
            throw new RuntimeException("이미 등록된 ISBN입니다: " + request.getIsbn());
        }

        // 필드별 업데이트 (null이 아닌 경우만)
        if (StringUtils.hasText(request.getTitle())) {
            book.updateTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getAuthor())) {
            book.updateAuthor(request.getAuthor());
        }
        if (StringUtils.hasText(request.getPublisher())) {
            book.updatePublisher(request.getPublisher());
        }
        if (StringUtils.hasText(request.getIsbn())) {
            book.updateIsbn(request.getIsbn());
        }
        if (request.getPublishedYear() != null) {
            book.updatePublishedYear(request.getPublishedYear());
        }
        if (request.getDescription() != null) {
            book.updateDescription(request.getDescription());
        }
        if (request.getTotalPages() != null) {
            book.updateTotalPages(request.getTotalPages());
        }
        if (request.getCategory() != null) {
            book.updateCategory(request.getCategory());
        }

        book = bookRepository.save(book);
        
        logger.info("Book updated: {}", book.getId());
        
        return convertToResponse(book);
    }

    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다: " + bookId));

        // 관련 데이터 확인 (사용자가 등록한 책이 있는 경우 삭제 방지)
        if (bookRepository.countUserBooksByBookId(bookId) > 0) {
            throw new RuntimeException("사용자들이 등록한 책은 삭제할 수 없습니다");
        }

        bookRepository.delete(book);
        
        logger.info("Book deleted: {}", bookId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookResponse> searchBooks(BookSearchRequest searchRequest) {
        Specification<Book> spec = createBookSpecification(searchRequest);
        
        Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
        Pageable pageable = PageRequest.of(
            searchRequest.getPage(), 
            searchRequest.getSize(), 
            sort
        );

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);
        
        List<BookResponse> bookResponses = bookPage.getContent().stream()
            .map(this::convertToResponse)
            .toList();

        return PagedResponse.<BookResponse>builder()
            .content(bookResponses)
            .pageNumber(bookPage.getNumber())
            .pageSize(bookPage.getSize())
            .totalElements(bookPage.getTotalElements())
            .totalPages(bookPage.getTotalPages())
            .first(bookPage.isFirst())
            .last(bookPage.isLast())
            .empty(bookPage.isEmpty())
            .build();
    }

    @Transactional(readOnly = true)
    public List<BookCategory> getCategories() {
        return Arrays.asList(BookCategory.values());
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getPopularBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> popularBooks = bookRepository.findPopularBooks(pageable);
        
        return popularBooks.stream()
            .map(this::convertToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getRecentBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> recentBooks = bookRepository.findAll(pageable);
        
        return recentBooks.getContent().stream()
            .map(this::convertToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getRecommendedBooks(Long userId, int limit) {
        // 향후 추천 알고리즘 구현 (현재는 인기 책을 반환)
        return getPopularBooks(limit);
    }

    private Specification<Book> createBookSpecification(BookSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 전체 텍스트 검색
            if (StringUtils.hasText(searchRequest.getQuery())) {
                String searchQuery = "%" + searchRequest.getQuery().toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), searchQuery);
                Predicate authorPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("author")), searchQuery);
                Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), searchQuery);
                
                predicates.add(criteriaBuilder.or(titlePredicate, authorPredicate, descriptionPredicate));
            }

            // 제목 검색
            if (StringUtils.hasText(searchRequest.getTitle())) {
                String titleQuery = "%" + searchRequest.getTitle().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), titleQuery));
            }

            // 저자 검색
            if (StringUtils.hasText(searchRequest.getAuthor())) {
                String authorQuery = "%" + searchRequest.getAuthor().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("author")), authorQuery));
            }

            // 출판사 검색
            if (StringUtils.hasText(searchRequest.getPublisher())) {
                String publisherQuery = "%" + searchRequest.getPublisher().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("publisher")), publisherQuery));
            }

            // ISBN 검색
            if (StringUtils.hasText(searchRequest.getIsbn())) {
                predicates.add(criteriaBuilder.equal(root.get("isbn"), searchRequest.getIsbn()));
            }

            // 카테고리 필터
            if (searchRequest.getCategory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), searchRequest.getCategory()));
            }

            // 출간년도 범위
            if (searchRequest.getPublishedYearFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("publishedYear"), searchRequest.getPublishedYearFrom()));
            }
            if (searchRequest.getPublishedYearTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("publishedYear"), searchRequest.getPublishedYearTo()));
            }

            // 페이지 수 범위
            if (searchRequest.getMinPages() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("totalPages"), searchRequest.getMinPages()));
            }
            if (searchRequest.getMaxPages() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("totalPages"), searchRequest.getMaxPages()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy) {
            case "title" -> Sort.by(direction, "title");
            case "author" -> Sort.by(direction, "author");
            case "publishedYear" -> Sort.by(direction, "publishedYear");
            case "totalPages" -> Sort.by(direction, "totalPages");
            default -> Sort.by(direction, "createdAt");
        };
    }

    private BookResponse convertToResponse(Book book) {
        return BookResponse.builder()
            .id(book.getId())
            .title(book.getTitle())
            .author(book.getAuthor())
            .publisher(book.getPublisher())
            .isbn(book.getIsbn())
            .publishedYear(book.getPublishedYear())
            .description(book.getDescription())
            .totalPages(book.getTotalPages())
            .category(book.getCategory())
            .createdAt(book.getCreatedAt())
            .updatedAt(book.getUpdatedAt())
            // 추가 통계는 필요시 쿼리로 조회
            .build();
    }
}