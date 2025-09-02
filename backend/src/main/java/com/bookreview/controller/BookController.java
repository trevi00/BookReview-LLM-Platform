package com.bookreview.controller;

import com.bookreview.domain.enums.BookCategory;
import com.bookreview.dto.book.*;
import com.bookreview.dto.common.ApiResponse;
import com.bookreview.dto.common.PagedResponse;
import com.bookreview.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Book Management", description = "책 관리 API")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @PostMapping
    @Operation(summary = "책 등록", description = "새로운 책을 등록합니다")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "책 등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 책")
    })
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Valid @RequestBody CreateBookRequest request) {
        
        try {
            BookResponse bookResponse = bookService.createBook(request);
            
            logger.info("New book created: {} by {}", request.getTitle(), request.getAuthor());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(bookResponse, "책이 성공적으로 등록되었습니다"));
                
        } catch (Exception e) {
            logger.error("Failed to create book: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "책 상세 조회", description = "특정 책의 상세 정보를 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "책 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<BookResponse>> getBook(
            @Parameter(description = "책 ID") @PathVariable Long bookId) {
        
        try {
            BookResponse bookResponse = bookService.getBook(bookId);
            
            return ResponseEntity.ok(ApiResponse.success(bookResponse));
            
        } catch (Exception e) {
            logger.error("Failed to get book {}: {}", bookId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "책 정보 수정", description = "기존 책의 정보를 수정합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 수정 가능
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "책 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @Parameter(description = "책 ID") @PathVariable Long bookId,
            @Valid @RequestBody UpdateBookRequest request) {
        
        try {
            BookResponse bookResponse = bookService.updateBook(bookId, request);
            
            logger.info("Book updated: {}", bookId);
            
            return ResponseEntity.ok(ApiResponse.success(bookResponse, "책 정보가 수정되었습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to update book {}: {}", bookId, e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "책 삭제", description = "책을 삭제합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 삭제 가능
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "책 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "삭제할 수 없는 책"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @Parameter(description = "책 ID") @PathVariable Long bookId) {
        
        try {
            bookService.deleteBook(bookId);
            
            logger.info("Book deleted: {}", bookId);
            
            return ResponseEntity.ok(ApiResponse.success(null, "책이 삭제되었습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to delete book {}: {}", bookId, e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "책 검색", description = "다양한 조건으로 책을 검색합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    })
    public ResponseEntity<ApiResponse<PagedResponse<BookResponse>>> searchBooks(
            @Parameter(description = "검색 조건") @ModelAttribute @Valid BookSearchRequest searchRequest) {
        
        try {
            PagedResponse<BookResponse> books = bookService.searchBooks(searchRequest);
            
            logger.debug("Book search completed. Found {} books", books.getTotalElements());
            
            return ResponseEntity.ok(ApiResponse.success(books, "책 검색이 완료되었습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to search books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("책 검색 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "책 카테고리 목록", description = "사용 가능한 모든 책 카테고리를 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<BookCategory>>> getCategories() {
        
        try {
            List<BookCategory> categories = bookService.getCategories();
            
            return ResponseEntity.ok(ApiResponse.success(categories, "카테고리 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get categories: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("카테고리 조회 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 책 목록", description = "많은 사용자들이 선택한 인기 책 목록을 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 책 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<BookResponse>>> getPopularBooks(
            @Parameter(description = "조회할 책 수") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10; // 기본값으로 설정
            }
            
            List<BookResponse> popularBooks = bookService.getPopularBooks(limit);
            
            return ResponseEntity.ok(ApiResponse.success(popularBooks, "인기 책 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get popular books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("인기 책 조회 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/recent")
    @Operation(summary = "최신 등록 책 목록", description = "최근에 등록된 책 목록을 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "최신 책 조회 성공")
    })
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRecentBooks(
            @Parameter(description = "조회할 책 수") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10; // 기본값으로 설정
            }
            
            List<BookResponse> recentBooks = bookService.getRecentBooks(limit);
            
            return ResponseEntity.ok(ApiResponse.success(recentBooks, "최신 책 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get recent books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("최신 책 조회 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/recommended")
    @Operation(summary = "추천 책 목록", description = "사용자 맞춤 추천 책 목록을 조회합니다")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 책 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRecommendedBooks(
            @Parameter(description = "조회할 책 수") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10; // 기본값으로 설정
            }
            
            // TODO: 현재 사용자 ID 가져오기
            Long currentUserId = 1L; // 임시로 하드코딩
            
            List<BookResponse> recommendedBooks = bookService.getRecommendedBooks(currentUserId, limit);
            
            return ResponseEntity.ok(ApiResponse.success(recommendedBooks, "추천 책 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get recommended books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("추천 책 조회 중 오류가 발생했습니다"));
        }
    }
}