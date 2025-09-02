package com.bookreview.service;

import com.bookreview.domain.Book;
import com.bookreview.domain.User;
import com.bookreview.domain.UserBook;
import com.bookreview.domain.enums.ReadingStatus;
import com.bookreview.dto.book.BookResponse;
import com.bookreview.dto.common.PagedResponse;
import com.bookreview.dto.userbook.*;
import com.bookreview.repository.BookRepository;
import com.bookreview.repository.UserBookRepository;
import com.bookreview.repository.UserRepository;
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

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserBookService {

    private static final Logger logger = LoggerFactory.getLogger(UserBookService.class);

    @Autowired
    private UserBookRepository userBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    public UserBookResponse addUserBook(Long userId, CreateUserBookRequest request) {
        // 사용자 존재 확인
        User user = userRepository.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 책 존재 확인
        Book book = bookRepository.findById(request.getBookId())
            .orElseThrow(() -> new RuntimeException("책을 찾을 수 없습니다: " + request.getBookId()));

        // 중복 등록 확인
        Optional<UserBook> existingUserBook = userBookRepository
            .findByUserIdAndBookId(userId, request.getBookId());
        if (existingUserBook.isPresent()) {
            throw new RuntimeException("이미 서재에 등록된 책입니다");
        }

        // 현재 페이지 유효성 검사
        if (request.getCurrentPage() != null && request.getCurrentPage() > book.getTotalPages()) {
            throw new RuntimeException("현재 페이지가 총 페이지 수를 초과합니다");
        }

        UserBook userBook = UserBook.builder()
            .user(user)
            .book(book)
            .status(request.getStatus())
            .startDate(request.getStartDate())
            .currentPage(request.getCurrentPage() != null ? request.getCurrentPage() : 0)
            .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
            .build();

        // 독서 시작 시 자동으로 시작일 설정
        if (request.getStatus() == ReadingStatus.READING && request.getStartDate() == null) {
            userBook.updateStartDate(LocalDateTime.now());
        }

        userBook = userBookRepository.save(userBook);
        
        logger.info("User {} added book {} to library", userId, request.getBookId());
        
        return convertToResponse(userBook);
    }

    @Transactional(readOnly = true)
    public UserBookResponse getUserBook(Long userId, Long userBookId) {
        UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
            .orElseThrow(() -> new RuntimeException("서재에서 책을 찾을 수 없습니다"));
        
        return convertToResponse(userBook);
    }

    public UserBookResponse updateUserBook(Long userId, Long userBookId, UpdateUserBookRequest request) {
        UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
            .orElseThrow(() -> new RuntimeException("서재에서 책을 찾을 수 없습니다"));

        // 현재 페이지 유효성 검사
        if (request.getCurrentPage() != null) {
            if (request.getCurrentPage() < 0) {
                throw new RuntimeException("현재 페이지는 0 이상이어야 합니다");
            }
            if (request.getCurrentPage() > userBook.getBook().getTotalPages()) {
                throw new RuntimeException("현재 페이지가 총 페이지 수를 초과합니다");
            }
        }

        // 필드별 업데이트
        if (request.getStatus() != null) {
            userBook.updateStatus(request.getStatus());
            
            // 상태에 따른 자동 날짜 설정
            if (request.getStatus() == ReadingStatus.READING && userBook.getStartDate() == null) {
                userBook.updateStartDate(LocalDateTime.now());
            } else if (request.getStatus() == ReadingStatus.COMPLETED && request.getEndDate() == null) {
                userBook.updateEndDate(LocalDateTime.now());
                // 완료 시 현재 페이지를 총 페이지로 설정
                if (request.getCurrentPage() == null) {
                    userBook.updateCurrentPage(userBook.getBook().getTotalPages());
                }
            }
        }

        if (request.getStartDate() != null) {
            userBook.updateStartDate(request.getStartDate());
        }
        
        if (request.getEndDate() != null) {
            userBook.updateEndDate(request.getEndDate());
        }
        
        if (request.getCurrentPage() != null) {
            userBook.updateCurrentPage(request.getCurrentPage());
        }
        
        if (request.getRating() != null) {
            userBook.updateRating(request.getRating());
        }
        
        if (request.getReview() != null) {
            userBook.updateReview(request.getReview());
        }
        
        if (request.getIsPrivate() != null) {
            userBook.updateIsPrivate(request.getIsPrivate());
        }

        userBook = userBookRepository.save(userBook);
        
        logger.info("User {} updated book {} in library", userId, userBookId);
        
        return convertToResponse(userBook);
    }

    public void removeUserBook(Long userId, Long userBookId) {
        UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
            .orElseThrow(() -> new RuntimeException("서재에서 책을 찾을 수 없습니다"));

        userBookRepository.delete(userBook);
        
        logger.info("User {} removed book {} from library", userId, userBookId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserBookResponse> getUserBooks(Long userId, UserBookSearchRequest searchRequest) {
        Specification<UserBook> spec = createUserBookSpecification(userId, searchRequest);
        
        Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
        Pageable pageable = PageRequest.of(
            searchRequest.getPage(), 
            searchRequest.getSize(), 
            sort
        );

        Page<UserBook> userBookPage = userBookRepository.findAll(spec, pageable);
        
        List<UserBookResponse> responses = userBookPage.getContent().stream()
            .map(this::convertToResponse)
            .toList();

        return PagedResponse.<UserBookResponse>builder()
            .content(responses)
            .pageNumber(userBookPage.getNumber())
            .pageSize(userBookPage.getSize())
            .totalElements(userBookPage.getTotalElements())
            .totalPages(userBookPage.getTotalPages())
            .first(userBookPage.isFirst())
            .last(userBookPage.isLast())
            .empty(userBookPage.isEmpty())
            .build();
    }

    @Transactional(readOnly = true)
    public List<UserBookResponse> getCurrentlyReading(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<UserBook> readingBooks = userBookRepository
            .findByUserIdAndStatus(userId, ReadingStatus.READING, pageable);
        
        return readingBooks.stream()
            .map(this::convertToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<UserBookResponse> getRecentlyCompleted(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "endDate"));
        List<UserBook> completedBooks = userBookRepository
            .findByUserIdAndStatus(userId, ReadingStatus.COMPLETED, pageable);
        
        return completedBooks.stream()
            .map(this::convertToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean isBookInUserLibrary(Long userId, Long bookId) {
        return userBookRepository.findByUserIdAndBookId(userId, bookId).isPresent();
    }

    private Specification<UserBook> createUserBookSpecification(Long userId, UserBookSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 사용자 필터 (필수)
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            // 독서 상태 필터
            if (searchRequest.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), searchRequest.getStatus()));
            }

            // 책 정보 조인 및 필터
            Join<UserBook, Book> bookJoin = root.join("book", JoinType.LEFT);

            if (StringUtils.hasText(searchRequest.getBookTitle())) {
                String titleQuery = "%" + searchRequest.getBookTitle().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(bookJoin.get("title")), titleQuery));
            }

            if (StringUtils.hasText(searchRequest.getBookAuthor())) {
                String authorQuery = "%" + searchRequest.getBookAuthor().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(bookJoin.get("author")), authorQuery));
            }

            if (searchRequest.getBookCategory() != null) {
                predicates.add(criteriaBuilder.equal(bookJoin.get("category"), searchRequest.getBookCategory()));
            }

            // 평점 필터
            if (searchRequest.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("rating"), searchRequest.getMinRating()));
            }
            if (searchRequest.getMaxRating() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("rating"), searchRequest.getMaxRating()));
            }

            // 날짜 필터
            if (searchRequest.getStartDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("startDate"), searchRequest.getStartDateFrom()));
            }
            if (searchRequest.getStartDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("startDate"), searchRequest.getStartDateTo()));
            }
            if (searchRequest.getEndDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("endDate"), searchRequest.getEndDateFrom()));
            }
            if (searchRequest.getEndDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("endDate"), searchRequest.getEndDateTo()));
            }

            // 공개/비공개 필터
            if (searchRequest.getIsPrivate() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPrivate"), searchRequest.getIsPrivate()));
            }

            // 리뷰 존재 여부 필터
            if (searchRequest.getHasReview() != null && searchRequest.getHasReview()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("review")));
                predicates.add(criteriaBuilder.notEqual(root.get("review"), ""));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy) {
            case "status" -> Sort.by(direction, "status");
            case "startDate" -> Sort.by(direction, "startDate");
            case "endDate" -> Sort.by(direction, "endDate");
            case "rating" -> Sort.by(direction, "rating");
            case "progress" -> Sort.by(direction, "currentPage");
            default -> Sort.by(direction, "updatedAt");
        };
    }

    private UserBookResponse convertToResponse(UserBook userBook) {
        // 진도율 계산
        Integer readingProgress = 0;
        if (userBook.getBook().getTotalPages() > 0) {
            readingProgress = (int) Math.round(
                (double) userBook.getCurrentPage() / userBook.getBook().getTotalPages() * 100);
        }

        // 독서 일수 계산
        Long totalReadingDays = 0L;
        if (userBook.getStartDate() != null) {
            LocalDateTime endDate = userBook.getEndDate() != null ? 
                userBook.getEndDate() : LocalDateTime.now();
            totalReadingDays = ChronoUnit.DAYS.between(userBook.getStartDate(), endDate);
        }

        // 책 정보 변환
        BookResponse bookResponse = BookResponse.builder()
            .id(userBook.getBook().getId())
            .title(userBook.getBook().getTitle())
            .author(userBook.getBook().getAuthor())
            .publisher(userBook.getBook().getPublisher())
            .isbn(userBook.getBook().getIsbn())
            .publishedYear(userBook.getBook().getPublishedYear())
            .description(userBook.getBook().getDescription())
            .totalPages(userBook.getBook().getTotalPages())
            .category(userBook.getBook().getCategory())
            .createdAt(userBook.getBook().getCreatedAt())
            .updatedAt(userBook.getBook().getUpdatedAt())
            .build();

        return UserBookResponse.builder()
            .id(userBook.getId())
            .userId(userBook.getUser().getId())
            .bookId(userBook.getBook().getId())
            .status(userBook.getStatus())
            .startDate(userBook.getStartDate())
            .endDate(userBook.getEndDate())
            .currentPage(userBook.getCurrentPage())
            .rating(userBook.getRating())
            .review(userBook.getReview())
            .isPrivate(userBook.getIsPrivate())
            .createdAt(userBook.getCreatedAt())
            .updatedAt(userBook.getUpdatedAt())
            .book(bookResponse)
            .readingProgress(readingProgress)
            .totalReadingDays(totalReadingDays)
            // TODO: 챕터 및 노트 통계는 별도 쿼리로 조회 (성능 최적화)
            .totalChapters(0L)
            .completedChapters(0L)
            .totalNotes(0L)
            .build();
    }
}