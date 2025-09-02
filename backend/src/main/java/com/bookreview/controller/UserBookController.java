package com.bookreview.controller;

import com.bookreview.dto.common.ApiResponse;
import com.bookreview.dto.common.PagedResponse;
import com.bookreview.dto.userbook.*;
import com.bookreview.security.CustomUserDetailsService;
import com.bookreview.service.UserBookService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-books")
@Tag(name = "User Book Management", description = "사용자 서재 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class UserBookController {

    private static final Logger logger = LoggerFactory.getLogger(UserBookController.class);

    @Autowired
    private UserBookService userBookService;

    @PostMapping
    @Operation(summary = "서재에 책 추가", description = "사용자 서재에 새로운 책을 추가합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "책 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록된 책")
    })
    public ResponseEntity<ApiResponse<UserBookResponse>> addUserBook(
            @Valid @RequestBody CreateUserBookRequest request) {
        
        try {
            Long userId = getCurrentUserId();
            UserBookResponse userBookResponse = userBookService.addUserBook(userId, request);
            
            logger.info("User {} added book {} to library", userId, request.getBookId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userBookResponse, "책이 서재에 추가되었습니다"));
                
        } catch (Exception e) {
            logger.error("Failed to add book to user library: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{userBookId}")
    @Operation(summary = "서재 책 상세 조회", description = "서재에 등록된 특정 책의 상세 정보를 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<UserBookResponse>> getUserBook(
            @Parameter(description = "사용자 책 ID") @PathVariable Long userBookId) {
        
        try {
            Long userId = getCurrentUserId();
            UserBookResponse userBookResponse = userBookService.getUserBook(userId, userBookId);
            
            return ResponseEntity.ok(ApiResponse.success(userBookResponse));
            
        } catch (Exception e) {
            logger.error("Failed to get user book {}: {}", userBookId, e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{userBookId}")
    @Operation(summary = "서재 책 정보 수정", description = "서재에 등록된 책의 정보를 수정합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<UserBookResponse>> updateUserBook(
            @Parameter(description = "사용자 책 ID") @PathVariable Long userBookId,
            @Valid @RequestBody UpdateUserBookRequest request) {
        
        try {
            Long userId = getCurrentUserId();
            UserBookResponse userBookResponse = userBookService.updateUserBook(userId, userBookId, request);
            
            logger.info("User {} updated book {} in library", userId, userBookId);
            
            return ResponseEntity.ok(ApiResponse.success(userBookResponse, "책 정보가 수정되었습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to update user book {}: {}", userBookId, e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{userBookId}")
    @Operation(summary = "서재에서 책 제거", description = "서재에서 책을 제거합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Void>> removeUserBook(
            @Parameter(description = "사용자 책 ID") @PathVariable Long userBookId) {
        
        try {
            Long userId = getCurrentUserId();
            userBookService.removeUserBook(userId, userBookId);
            
            logger.info("User {} removed book {} from library", userId, userBookId);
            
            return ResponseEntity.ok(ApiResponse.success(null, "책이 서재에서 제거되었습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to remove user book {}: {}", userBookId, e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "서재 책 목록 조회", description = "사용자의 서재에 등록된 책 목록을 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<PagedResponse<UserBookResponse>>> getUserBooks(
            @Parameter(description = "검색 조건") @ModelAttribute @Valid UserBookSearchRequest searchRequest) {
        
        try {
            Long userId = getCurrentUserId();
            PagedResponse<UserBookResponse> userBooks = userBookService.getUserBooks(userId, searchRequest);
            
            logger.debug("Retrieved {} books for user {}", userBooks.getTotalElements(), userId);
            
            return ResponseEntity.ok(ApiResponse.success(userBooks, "서재 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get user books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("서재 목록 조회 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/currently-reading")
    @Operation(summary = "현재 읽고 있는 책", description = "현재 읽고 있는 책 목록을 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<List<UserBookResponse>>> getCurrentlyReading(
            @Parameter(description = "조회할 책 수") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10; // 기본값으로 설정
            }
            
            Long userId = getCurrentUserId();
            List<UserBookResponse> currentlyReading = userBookService.getCurrentlyReading(userId, limit);
            
            return ResponseEntity.ok(ApiResponse.success(currentlyReading, "현재 읽고 있는 책 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get currently reading books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("현재 읽고 있는 책 조회 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/recently-completed")
    @Operation(summary = "최근 완료한 책", description = "최근에 완료한 책 목록을 조회합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<List<UserBookResponse>>> getRecentlyCompleted(
            @Parameter(description = "조회할 책 수") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10; // 기본값으로 설정
            }
            
            Long userId = getCurrentUserId();
            List<UserBookResponse> recentlyCompleted = userBookService.getRecentlyCompleted(userId, limit);
            
            return ResponseEntity.ok(ApiResponse.success(recentlyCompleted, "최근 완료한 책 목록을 조회했습니다"));
            
        } catch (Exception e) {
            logger.error("Failed to get recently completed books: {}", e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("최근 완료한 책 조회 중 오류가 발생했습니다"));
        }
    }

    @GetMapping("/check/{bookId}")
    @Operation(summary = "서재 등록 여부 확인", description = "특정 책이 서재에 등록되어 있는지 확인합니다")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<Boolean>> isBookInLibrary(
            @Parameter(description = "책 ID") @PathVariable Long bookId) {
        
        try {
            Long userId = getCurrentUserId();
            boolean isInLibrary = userBookService.isBookInUserLibrary(userId, bookId);
            
            return ResponseEntity.ok(ApiResponse.success(isInLibrary, 
                isInLibrary ? "서재에 등록된 책입니다" : "서재에 등록되지 않은 책입니다"));
            
        } catch (Exception e) {
            logger.error("Failed to check if book {} is in user library: {}", bookId, e.getMessage());
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("서재 등록 여부 확인 중 오류가 발생했습니다"));
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetailsService.UserPrincipal userPrincipal = 
            (CustomUserDetailsService.UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
}