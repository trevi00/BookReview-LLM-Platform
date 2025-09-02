# 논리적 설계 - API 설계 및 시스템 아키텍처

## 1. 전체 시스템 아키텍처

### 1.1 아키텍처 개요
```
┌─────────────────┐    ┌─────────────────┐
│  React Native   │    │   Web Browser   │
│   Mobile App    │    │   (React Web)   │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────┬───────────┘
                     │
              ┌─────────────┐
              │ API Gateway │
              └─────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
  ┌─────────────┐ ┌──────────┐ ┌──────────────┐
  │ Spring Boot │ │ FastAPI  │ │   MySQL      │
  │   Backend   │ │ AI Service│ │   Database   │
  └─────────────┘ └──────────┘ └──────────────┘
         │           │           │
  ┌─────────────┐ ┌──────────┐ ┌──────────────┐
  │   Redis     │ │ OpenAI   │ │    Docker    │
  │   Cache     │ │   API    │ │  Container   │
  └─────────────┘ └──────────┘ └──────────────┘
```

### 1.2 계층형 아키텍처 (Spring Boot)
```
┌─────────────────────────────────────┐
│           Presentation Layer        │
│  (Controllers, DTOs, Exception)     │
├─────────────────────────────────────┤
│            Service Layer            │
│   (Business Logic, Transactions)    │
├─────────────────────────────────────┤
│           Repository Layer          │
│      (Data Access, JPA)            │
├─────────────────────────────────────┤
│             Domain Layer            │
│     (Entities, Value Objects)       │
└─────────────────────────────────────┘
```

### 1.3 마이크로서비스 간 통신
- **동기 통신**: REST API (Spring Boot ↔ FastAPI)
- **비동기 통신**: 메시지 큐 (향후 확장)
- **데이터 일관성**: 이벤트 소싱 (향후 확장)

## 2. API 설계

### 2.1 RESTful API 원칙
- HTTP 메서드 활용 (GET, POST, PUT, DELETE)
- 리소스 중심 URL 설계
- 상태 코드 표준화
- JSON 형태 응답
- 페이지네이션 지원

### 2.2 API 버전 관리
- URL 경로 버전: `/api/v1/`
- 하위 호환성 유지
- 점진적 마이그레이션

### 2.3 Spring Boot API 엔드포인트

#### 2.3.1 인증 API
```http
POST /api/v1/auth/login
POST /api/v1/auth/register
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
GET  /api/v1/auth/profile
PUT  /api/v1/auth/profile

# OAuth
GET  /api/v1/auth/oauth/google
GET  /api/v1/auth/oauth/callback/google
```

#### 2.3.2 책 관리 API
```http
GET    /api/v1/books?search={query}&page={page}
POST   /api/v1/books
GET    /api/v1/books/{bookId}
PUT    /api/v1/books/{bookId}
DELETE /api/v1/books/{bookId}

# 사용자-책 연결
GET    /api/v1/users/{userId}/books
POST   /api/v1/users/{userId}/books
GET    /api/v1/users/{userId}/books/{userBookId}
PUT    /api/v1/users/{userId}/books/{userBookId}
DELETE /api/v1/users/{userId}/books/{userBookId}
```

#### 2.3.3 목차 관리 API
```http
GET    /api/v1/user-books/{userBookId}/chapters
POST   /api/v1/user-books/{userBookId}/chapters
GET    /api/v1/user-books/{userBookId}/chapters/{chapterId}
PUT    /api/v1/user-books/{userBookId}/chapters/{chapterId}
DELETE /api/v1/user-books/{userBookId}/chapters/{chapterId}
```

#### 2.3.4 독서 기록 API
```http
GET    /api/v1/chapters/{chapterId}/notes
POST   /api/v1/chapters/{chapterId}/notes
GET    /api/v1/notes/{noteId}
PUT    /api/v1/notes/{noteId}
DELETE /api/v1/notes/{noteId}

# 사용자별 모든 기록
GET    /api/v1/users/{userId}/notes?page={page}&type={type}
```

#### 2.3.5 독서 세션 API
```http
GET    /api/v1/user-books/{userBookId}/sessions
POST   /api/v1/user-books/{userBookId}/sessions
PUT    /api/v1/sessions/{sessionId}
DELETE /api/v1/sessions/{sessionId}
```

#### 2.3.6 통계 및 분석 API
```http
GET /api/v1/users/{userId}/analytics/dashboard
GET /api/v1/users/{userId}/analytics/reading-progress
GET /api/v1/users/{userId}/analytics/monthly-report?year={year}&month={month}
GET /api/v1/users/{userId}/goals
POST /api/v1/users/{userId}/goals
PUT /api/v1/users/{userId}/goals/{goalId}
```

### 2.4 FastAPI AI 서비스 엔드포인트

#### 2.4.1 피드백 생성 API
```http
POST /api/v1/ai/feedback/generate
POST /api/v1/ai/feedback/batch
GET  /api/v1/ai/feedback/{feedbackId}
PUT  /api/v1/ai/feedback/{feedbackId}/rating
```

#### 2.4.2 질문 생성 API
```http
POST /api/v1/ai/questions/generate
POST /api/v1/ai/questions/follow-up
```

#### 2.4.3 텍스트 분석 API
```http
POST /api/v1/ai/analysis/sentiment
POST /api/v1/ai/analysis/summary
POST /api/v1/ai/analysis/keywords
```

### 2.5 API 요청/응답 예시

#### 2.5.1 독서 기록 생성
```http
POST /api/v1/chapters/123/notes
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "content": "스프링 시큐리티의 필터 체인에 대해 학습했다. 특히 AuthenticationFilter의 동작 원리가 흥미로웠다.",
  "noteType": "LEARNING",
  "pageNumber": 45,
  "isPrivate": false
}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "success": true,
  "data": {
    "id": 456,
    "chapterId": 123,
    "content": "스프링 시큐리티의 필터 체인에 대해 학습했다. 특히 AuthenticationFilter의 동작 원리가 흥미로웠다.",
    "noteType": "LEARNING",
    "pageNumber": 45,
    "isPrivate": false,
    "createdAt": "2024-01-20T10:30:00Z"
  },
  "message": "독서 기록이 성공적으로 생성되었습니다."
}
```

#### 2.5.2 AI 피드백 생성
```http
POST /api/v1/ai/feedback/generate
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "noteId": 456,
  "feedbackType": "COMMENT",
  "context": {
    "bookTitle": "스프링 부트 실전 활용 마스터",
    "chapterTitle": "스프링 시큐리티",
    "noteContent": "스프링 시큐리티의 필터 체인에 대해 학습했다."
  }
}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "data": {
    "id": 789,
    "noteId": 456,
    "content": "스프링 시큐리티의 필터 체인은 정말 중요한 개념입니다. AuthenticationFilter 외에도 AuthorizationFilter, ExceptionTranslationFilter 등이 어떻게 연계되는지 살펴보시면 더 깊이 이해할 수 있을 것 같습니다. 실제 프로젝트에서는 어떤 부분을 커스터마이징하고 싶으신가요?",
    "feedbackType": "COMMENT",
    "aiModel": "gpt-4",
    "createdAt": "2024-01-20T10:31:00Z"
  },
  "message": "AI 피드백이 생성되었습니다."
}
```

## 3. 데이터 전송 객체 (DTO) 설계

### 3.1 Request DTOs

#### 3.1.1 UserRegistrationRequest
```java
public class UserRegistrationRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8-20자여야 합니다")
    private String password;
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 20, message = "사용자명은 2-20자여야 합니다")
    private String username;
}
```

#### 3.1.2 BookCreationRequest
```java
public class BookCreationRequest {
    @NotBlank(message = "제목은 필수입니다")
    private String title;
    
    @NotBlank(message = "저자는 필수입니다")
    private String author;
    
    private String publisher;
    private String isbn;
    private Integer publishedYear;
    private String description;
    private String coverImageUrl;
    private Integer totalPages;
    
    @NotNull(message = "카테고리는 필수입니다")
    private BookCategory category;
}
```

#### 3.1.3 ReadingNoteRequest
```java
public class ReadingNoteRequest {
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10,000자를 초과할 수 없습니다")
    private String content;
    
    @NotNull(message = "노트 타입은 필수입니다")
    private NoteType noteType;
    
    @Positive(message = "페이지 번호는 양수여야 합니다")
    private Integer pageNumber;
    
    private Boolean isPrivate = false;
}
```

### 3.2 Response DTOs

#### 3.2.1 ApiResponse (공통 응답 래퍼)
```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
}
```

#### 3.2.2 UserResponse
```java
public class UserResponse {
    private Long id;
    private String email;
    private String username;
    private String profileImage;
    private AuthProvider provider;
    private LocalDateTime createdAt;
}
```

#### 3.2.3 BookDetailResponse
```java
public class BookDetailResponse {
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private Integer publishedYear;
    private String description;
    private String coverImageUrl;
    private Integer totalPages;
    private BookCategory category;
    private LocalDateTime createdAt;
}
```

### 3.3 페이지네이션 응답
```java
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

## 4. 보안 아키텍처

### 4.1 인증/인가 플로우
```
1. 클라이언트 로그인 요청
2. Spring Security 인증 처리
3. JWT 토큰 발급
4. 토큰을 헤더에 포함하여 API 요청
5. JWT 필터에서 토큰 검증
6. SecurityContext에 인증 정보 저장
7. 컨트롤러 메서드 실행
```

### 4.2 JWT 토큰 구조
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user@example.com",
    "userId": 123,
    "roles": ["USER"],
    "iat": 1642665600,
    "exp": 1642752000
  }
}
```

### 4.3 OAuth 2.0 플로우 (Google)
```
1. 프론트엔드 → Google OAuth 인증 페이지
2. 사용자 인증 완료 → 인증 코드 반환
3. 백엔드 → Google API로 토큰 교환
4. Google API → 사용자 정보 반환
5. 백엔드 → 사용자 정보로 회원가입/로그인 처리
6. 백엔드 → JWT 토큰 발급
```

## 5. 에러 처리 전략

### 5.1 예외 계층 구조
```
BusinessException
├── UserNotFoundException
├── BookNotFoundException
├── DuplicateEmailException
├── InvalidCredentialsException
└── InsufficientPermissionException

TechnicalException
├── DatabaseConnectionException
├── ExternalApiException
└── FileProcessingException
```

### 5.2 글로벌 예외 처리
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ApiResponse<Object> response = ApiResponse.error(e.getMessage(), e.getErrorCode());
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException e) {
        // 유효성 검사 오류 처리
    }
}
```

### 5.3 에러 응답 형식
```json
{
  "success": false,
  "data": null,
  "message": "해당 사용자를 찾을 수 없습니다.",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2024-01-20T10:30:00Z"
}
```

## 6. 캐싱 전략

### 6.1 Redis 캐시 활용
- **세션 캐시**: 사용자 세션 정보
- **조회 캐시**: 자주 조회되는 책 정보
- **통계 캐시**: 계산 비용이 높은 통계 데이터
- **AI 응답 캐시**: 유사한 질문에 대한 AI 응답

### 6.2 캐시 키 전략
```
user:{userId}:profile
book:{bookId}:details
user:{userId}:reading-stats
ai:feedback:{contentHash}
```

### 6.3 캐시 만료 정책
- 사용자 프로필: 1시간
- 책 정보: 24시간
- 독서 통계: 30분
- AI 피드백: 7일

## 7. 로깅 및 모니터링

### 7.1 로그 레벨 정의
- **ERROR**: 시스템 오류, 예외 상황
- **WARN**: 경고, 잠재적 문제
- **INFO**: 중요한 비즈니스 이벤트
- **DEBUG**: 개발 디버깅 정보

### 7.2 로그 형식 (JSON)
```json
{
  "timestamp": "2024-01-20T10:30:00Z",
  "level": "INFO",
  "logger": "com.bookreview.service.UserService",
  "message": "사용자 등록 완료",
  "userId": 123,
  "email": "user@example.com",
  "traceId": "abc123"
}
```

### 7.3 메트릭 수집
- API 응답 시간
- 데이터베이스 쿼리 성능
- AI API 호출 수 및 응답 시간
- 사용자 활동 지표

## 8. 테스트 전략

### 8.1 테스트 피라미드
```
     E2E Tests
    ┌─────────────┐
   ┌─────────────────┐
  ┌───────────────────────┐
 │   Integration Tests   │
├─────────────────────────┤
│      Unit Tests        │
└─────────────────────────┘
```

### 8.2 단위 테스트 (JUnit 5)
- Service 계층 로직 테스트
- Repository 계층 데이터 접근 테스트
- Utility 클래스 테스트
- 커버리지 목표: 80% 이상

### 8.3 통합 테스트
- API 엔드포인트 테스트 (MockMvc)
- 데이터베이스 연동 테스트 (TestContainers)
- 외부 API 연동 테스트 (WireMock)

### 8.4 E2E 테스트
- 사용자 시나리오 기반 테스트
- 프론트엔드 + 백엔드 통합 테스트
- 성능 테스트 포함

## 9. 배포 아키텍처

### 9.1 컨테이너화 전략
```dockerfile
# Spring Boot Dockerfile
FROM openjdk:21-jre-slim
COPY target/bookreview-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 9.2 Docker Compose 구성
```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - database
      - redis
      - ai-service
  
  ai-service:
    build: ./ai-service
    ports:
      - "8000:8000"
  
  database:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: bookreview
    volumes:
      - mysql_data:/var/lib/mysql
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

### 9.3 환경별 설정
- **개발환경**: 로컬 Docker
- **테스트환경**: AWS ECS/EKS
- **운영환경**: 클라우드 배포 (향후 결정)