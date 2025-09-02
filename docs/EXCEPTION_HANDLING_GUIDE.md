# 예외 처리 시스템 가이드

## 📋 개요

BookReview-LLM-Platform 백엔드의 통합된 예외 처리 시스템에 대한 개발자 가이드입니다. 새로운 글로벌 예외 처리 아키텍처를 통해 일관성 있고 사용자 친화적인 에러 응답을 제공합니다.

---

## 🏗️ 예외 처리 아키텍처

### 시스템 구조
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controller    │    │  Global Handler │    │   Error Code    │
│                 │    │                 │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ BusinessLogic   │───▶│ Exception       │───▶│ Standardized    │
│ Validation      │    │ Interceptor     │    │ Response        │
│ Authentication  │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                        │                        │
        ▼                        ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Custom          │    │ @RestController │    │ ApiResponse<T>  │
│ Exceptions      │    │ Advice          │    │ DTO             │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 주요 구성 요소

1. **GlobalExceptionHandler** - 중앙집중식 예외 처리
2. **ErrorCode** - 표준화된 에러 코드 체계  
3. **Custom Exceptions** - 도메인별 커스텀 예외
4. **ApiResponse** - 통일된 응답 형식

---

## 🎯 ErrorCode 체계

### 에러 코드 구조
```java
@Getter @RequiredArgsConstructor
public enum ErrorCode {
    // [카테고리][일련번호] 형식
    USER_NOT_FOUND("USER001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BOOK_NOT_FOUND("BOOK001", "책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
}
```

### 카테고리별 에러 코드

#### 🔐 인증/인가 (AUTH)
| 코드 | 메시지 | HTTP 상태 | 설명 |
|------|--------|-----------|------|
| AUTH001 | 인증에 실패했습니다 | 401 | 일반적인 인증 실패 |
| AUTH002 | 유효하지 않은 JWT 토큰입니다 | 401 | 토큰 형식 오류 |
| AUTH003 | 만료된 JWT 토큰입니다 | 401 | 토큰 만료 |
| AUTH008 | 블랙리스트에 등록된 토큰입니다 | 401 | 무효화된 토큰 |

#### 👤 사용자 (USER)  
| 코드 | 메시지 | HTTP 상태 | 설명 |
|------|--------|-----------|------|
| USER001 | 사용자를 찾을 수 없습니다 | 404 | 존재하지 않는 사용자 |
| USER002 | 이미 존재하는 이메일입니다 | 409 | 이메일 중복 |
| USER003 | 비밀번호가 올바르지 않습니다 | 400 | 비밀번호 불일치 |
| USER004 | 보안이 약한 비밀번호입니다 | 400 | 비밀번호 정책 위반 |

#### 📚 도서 (BOOK)
| 코드 | 메시지 | HTTP 상태 | 설명 |
|------|--------|-----------|------|
| BOOK001 | 책을 찾을 수 없습니다 | 404 | 존재하지 않는 도서 |
| BOOK002 | 이미 존재하는 책입니다 | 409 | 도서 중복 등록 |
| BOOK003 | 유효하지 않은 ISBN입니다 | 400 | ISBN 형식 오류 |

#### 📝 독서노트 (NOTE)
| 코드 | 메시지 | HTTP 상태 | 설명 |
|------|--------|-----------|------|
| NOTE001 | 독서 노트를 찾을 수 없습니다 | 404 | 존재하지 않는 노트 |
| NOTE002 | 독서 노트에 접근할 권한이 없습니다 | 403 | 권한 없음 |
| NOTE004 | 노트 내용이 너무 깁니다 | 400 | 길이 제한 초과 |

---

## 🔨 커스텀 예외 클래스

### 기본 BusinessException
```java
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

### 특화된 예외 클래스

#### NotFoundException
```java
public class NotFoundException extends BusinessException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    // 사용 예시
    public static NotFoundException userNotFound(Long userId) {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND, 
            "사용자를 찾을 수 없습니다. ID: " + userId);
    }
}
```

#### UnauthorizedException  
```java
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    // 사용 예시
    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCode.INVALID_JWT_TOKEN);
    }
}
```

#### ConflictException
```java  
public class ConflictException extends BusinessException {
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    // 사용 예시
    public static ConflictException duplicateEmail(String email) {
        return new ConflictException(ErrorCode.DUPLICATE_EMAIL,
            "이미 사용 중인 이메일입니다: " + email);
    }
}
```

---

## 🌐 GlobalExceptionHandler

### 핸들러 구조
```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 비즈니스 로직 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        
        log.warn("Business exception: {} at {}", e.getMessage(), request.getRequestURI());
        
        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }
}
```

### 처리되는 예외 유형

#### 1. 비즈니스 로직 예외
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    // 커스텀 비즈니스 예외 처리
    return ResponseEntity
        .status(e.getErrorCode().getHttpStatus())
        .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
}
```

#### 2. 검증 예외
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException e) {
    
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach(error -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });
    
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("VALIDATION_FAILED", "입력값 검증에 실패했습니다.", errors));
}
```

#### 3. 인증/인가 예외
```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
        AuthenticationException e) {
    
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("AUTH_FAILED", "인증에 실패했습니다."));
}
```

#### 4. 시스템 예외
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleGenericException(
        Exception e, HttpServletRequest request) {
    
    log.error("Unexpected error: {} at {}", e.getMessage(), request.getRequestURI(), e);
    
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
}
```

---

## 📤 표준 응답 형식

### ApiResponse 구조
```java
@Getter @Builder
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(code)
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

### 응답 예시

#### ✅ 성공 응답
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Clean Code",
    "author": "Robert C. Martin"
  },
  "timestamp": 1690123456789
}
```

#### ❌ 에러 응답
```json
{
  "success": false,
  "code": "USER001",
  "message": "사용자를 찾을 수 없습니다.",
  "timestamp": 1690123456789
}
```

#### ⚠️ 검증 실패 응답
```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "입력값 검증에 실패했습니다.",
  "data": {
    "email": "이메일 형식이 올바르지 않습니다.",
    "password": "비밀번호는 8자 이상이어야 합니다."
  },
  "timestamp": 1690123456789
}
```

---

## 🔧 개발자 가이드

### 새로운 예외 추가하기

#### 1. ErrorCode에 새 에러 추가
```java
public enum ErrorCode {
    // 기존 코드들...
    
    // 새로운 에러 코드 추가
    CHAPTER_NOT_FOUND("CHAPTER001", "챕터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CHAPTER_ORDER("CHAPTER002", "유효하지 않은 챕터 순서입니다.", HttpStatus.BAD_REQUEST);
}
```

#### 2. 서비스에서 예외 사용
```java
@Service
public class ChapterService {
    
    public Chapter getChapter(Long chapterId) {
        return chapterRepository.findById(chapterId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CHAPTER_NOT_FOUND));
    }
    
    public void validateChapterOrder(int order) {
        if (order < 1) {
            throw new BusinessException(ErrorCode.INVALID_CHAPTER_ORDER, 
                "챕터 순서는 1 이상이어야 합니다. 입력값: " + order);
        }
    }
}
```

### 컨트롤러에서 예외 처리

#### ❌ 잘못된 방법
```java
@GetMapping("/users/{id}")
public ResponseEntity<?> getUser(@PathVariable Long id) {
    try {
        User user = userService.getUser(id);
        return ResponseEntity.ok(user);
    } catch (Exception e) {
        // 컨트롤러에서 직접 예외 처리 (권장하지 않음)
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    }
}
```

#### ✅ 올바른 방법
```java
@GetMapping("/users/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
    // 서비스에서 예외를 발생시키면 GlobalExceptionHandler가 자동 처리
    User user = userService.getUser(id);
    UserResponse response = UserResponse.from(user);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

### 검증 어노테이션 활용

#### DTO 검증
```java
@Getter @Setter
public class CreateBookRequest {
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
    private String title;
    
    @NotBlank(message = "저자는 필수입니다.")
    private String author;
    
    @ValidISBN(message = "유효하지 않은 ISBN입니다.")
    private String isbn;
    
    @NoXSS(message = "XSS 패턴이 감지되었습니다.")
    private String description;
}
```

#### 컨트롤러에서 검증
```java
@PostMapping("/books")
public ResponseEntity<ApiResponse<BookResponse>> createBook(
        @Valid @RequestBody CreateBookRequest request) {
    
    // @Valid가 실패하면 MethodArgumentNotValidException 발생
    // GlobalExceptionHandler가 자동으로 검증 에러 응답 생성
    Book book = bookService.createBook(request);
    return ResponseEntity.ok(ApiResponse.success(BookResponse.from(book)));
}
```

---

## 🧪 테스트 가이드

### 예외 처리 단위 테스트

#### 서비스 예외 테스트
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 NotFoundException 발생")
    void getUserNotFound() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, 
            () -> userService.getUser(userId));
        
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
```

#### 컨트롤러 예외 테스트
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 404 응답")
    void getUserNotFound() throws Exception {
        // Given
        Long userId = 1L;
        when(userService.getUser(userId))
            .thenThrow(new NotFoundException(ErrorCode.USER_NOT_FOUND));
        
        // When & Then
        mockMvc.perform(get("/api/users/{id}", userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("USER001"))
            .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }
}
```

### 검증 테스트
```java
@WebMvcTest(BookController.class)
class BookValidationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("잘못된 도서 생성 요청 시 검증 에러")
    void createBookValidationError() throws Exception {
        // Given
        CreateBookRequest invalidRequest = CreateBookRequest.builder()
            .title("") // 빈 제목
            .author("") // 빈 저자
            .isbn("invalid-isbn") // 잘못된 ISBN
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.data.title").value("제목은 필수입니다."))
            .andExpect(jsonPath("$.data.author").value("저자는 필수입니다."));
    }
}
```

---

## 📊 로깅 및 모니터링

### 예외 로깅 전략

#### 로그 레벨별 분류
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        // 비즈니스 예외는 WARN 레벨 (예상 가능한 예외)
        log.warn("Business exception: {}", e.getMessage());
        return createErrorResponse(e);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        // 시스템 예외는 ERROR 레벨 (예상하지 못한 예외)
        log.error("Unexpected error occurred", e);
        return createErrorResponse(INTERNAL_SERVER_ERROR);
    }
}
```

#### 구조화된 로깅
```java
// Logback에서 JSON 형태로 로깅
{
  "timestamp": "2025-07-23T10:30:00.000Z",
  "level": "WARN",
  "logger": "com.bookreview.exception.GlobalExceptionHandler",
  "message": "Business exception occurred",
  "exception": {
    "type": "NotFoundException",
    "code": "USER001",
    "message": "사용자를 찾을 수 없습니다."
  },
  "request": {
    "uri": "/api/users/999",
    "method": "GET",
    "userAgent": "Mozilla/5.0..."
  }
}
```

### 메트릭스 수집

#### Micrometer를 통한 예외 카운팅
```java
@Component
public class ExceptionMetrics {
    
    private final Counter businessExceptionCounter;
    private final Counter validationExceptionCounter;
    
    public ExceptionMetrics(MeterRegistry meterRegistry) {
        this.businessExceptionCounter = Counter.builder("exceptions.business")
            .description("Business exceptions count")
            .register(meterRegistry);
            
        this.validationExceptionCounter = Counter.builder("exceptions.validation")
            .description("Validation exceptions count")
            .register(meterRegistry);
    }
    
    public void incrementBusinessException(String errorCode) {
        businessExceptionCounter.increment(Tags.of("code", errorCode));
    }
}
```

---

## 📚 모범 사례 (Best Practices)

### ✅ Do's (권장사항)

1. **명확한 에러 메시지 작성**
   ```java
   // Good: 구체적이고 도움이 되는 메시지
   throw new NotFoundException(ErrorCode.BOOK_NOT_FOUND, 
       "ID " + bookId + "에 해당하는 책을 찾을 수 없습니다.");
   ```

2. **적절한 HTTP 상태 코드 사용**
   ```java
   // 404: 리소스 없음
   BOOK_NOT_FOUND("BOOK001", "책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
   
   // 409: 중복/충돌
   DUPLICATE_EMAIL("USER002", "이미 존재하는 이메일입니다.", HttpStatus.CONFLICT),
   
   // 400: 잘못된 요청
   INVALID_ISBN("BOOK003", "유효하지 않은 ISBN입니다.", HttpStatus.BAD_REQUEST)
   ```

3. **비즈니스 로직에서 예외 발생**
   ```java
   @Service
   public class BookService {
       public Book getBook(Long id) {
           return bookRepository.findById(id)
               .orElseThrow(() -> new NotFoundException(ErrorCode.BOOK_NOT_FOUND));
       }
   }
   ```

### ❌ Don'ts (금지사항)

1. **컨트롤러에서 직접 예외 처리하지 않기**
   ```java
   // Bad: 컨트롤러에서 try-catch 사용
   @GetMapping("/books/{id}")
   public ResponseEntity<?> getBook(@PathVariable Long id) {
       try {
           Book book = bookService.getBook(id);
           return ResponseEntity.ok(book);
       } catch (Exception e) {
           return ResponseEntity.badRequest().body("Error occurred");
       }
   }
   ```

2. **일반적인 Exception 클래스 직접 사용하지 않기**
   ```java
   // Bad: 일반적인 예외 사용
   throw new RuntimeException("Something went wrong");
   
   // Good: 구체적인 커스텀 예외 사용
   throw new BusinessException(ErrorCode.BOOK_CREATION_FAILED);
   ```

3. **민감한 정보 노출하지 않기**
   ```java
   // Bad: 내부 시스템 정보 노출
   throw new BusinessException(ERROR_CODE, 
       "Database connection failed: " + dbConnection.getUrl());
   
   // Good: 일반적인 메시지
   throw new BusinessException(ERROR_CODE, "데이터 처리 중 오류가 발생했습니다.");
   ```

---

## 📞 문의 및 지원

**개발팀 연락처**: dev-team@bookreview.com  
**기술 문의**: tech-support@bookreview.com  

예외 처리 시스템 관련 문의나 개선 제안이 있으시면 언제든지 연락해 주시기 바랍니다.

**문서 버전**: v1.0  
**최종 업데이트**: 2025-07-23