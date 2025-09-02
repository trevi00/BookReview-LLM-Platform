# 테스트 전략

## 목차
1. [테스트 철학](#테스트-철학)
2. [테스트 피라미드](#테스트-피라미드)
3. [백엔드 테스트](#백엔드-테스트)
4. [AI 서비스 테스트](#ai-서비스-테스트)
5. [프론트엔드 테스트](#프론트엔드-테스트)
6. [테스트 환경 설정](#테스트-환경-설정)
7. [테스트 데이터 관리](#테스트-데이터-관리)
8. [성능 테스트](#성능-테스트)
9. [테스트 자동화](#테스트-자동화)

## 테스트 철학

### TDD (Test-Driven Development) 적용
1. **Red**: 실패하는 테스트 작성
2. **Green**: 테스트를 통과하는 최소한의 코드 작성
3. **Refactor**: 코드 품질 개선

### 테스트 품질 기준
- **커버리지**: 최소 80% 이상
- **가독성**: 테스트 코드도 프로덕션 코드만큼 중요
- **독립성**: 각 테스트는 독립적으로 실행 가능
- **반복성**: 언제든 동일한 결과 보장

## 테스트 피라미드

```
        /\
       /  \      E2E 테스트 (5%)
      /____\     - 전체 시스템 통합 테스트
     /      \
    /        \   통합 테스트 (15%)
   /__________\  - API, 데이터베이스 통합
  /            \
 /              \ 단위 테스트 (80%)
/________________\ - 개별 함수, 클래스 테스트
```

### 테스트 유형별 비율
- **단위 테스트**: 80% - 빠르고 안정적
- **통합 테스트**: 15% - 컴포넌트 간 상호작용
- **E2E 테스트**: 5% - 사용자 시나리오

## 백엔드 테스트

### 단위 테스트

#### 서비스 계층 테스트
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {
    
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChapterRepository chapterRepository;
    
    @InjectMocks private BookService bookService;
    
    @Nested
    @DisplayName("책 생성")
    class CreateBook {
        
        @Test
        @DisplayName("유효한 데이터로 책 생성 시 성공한다")
        void createBook_WithValidData_ShouldReturnSavedBook() {
            // Given
            CreateBookRequest request = CreateBookRequest.builder()
                .title("클린 코드")
                .author("로버트 C. 마틴")
                .publisher("인사이트")
                .totalPages(584)
                .category(BookCategory.TECHNOLOGY)
                .build();
                
            Book expectedBook = Book.builder()
                .id(1L)
                .title("클린 코드")
                .author("로버트 C. 마틴")
                .publisher("인사이트")
                .totalPages(584)
                .category(BookCategory.TECHNOLOGY)
                .build();
                
            when(bookRepository.save(any(Book.class))).thenReturn(expectedBook);
            
            // When
            Book actualBook = bookService.createBook(request);
            
            // Then
            assertThat(actualBook).isNotNull();
            assertThat(actualBook.getTitle()).isEqualTo("클린 코드");
            assertThat(actualBook.getAuthor()).isEqualTo("로버트 C. 마틴");
            
            verify(bookRepository).save(any(Book.class));
        }
        
        @Test
        @DisplayName("제목이 null인 경우 예외가 발생한다")
        void createBook_WithNullTitle_ShouldThrowException() {
            // Given
            CreateBookRequest request = CreateBookRequest.builder()
                .title(null)
                .author("테스트 저자")
                .build();
            
            // When & Then
            assertThatThrownBy(() -> bookService.createBook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("책 제목은 필수입니다.");
        }
    }
    
    @Nested
    @DisplayName("사용자 책 등록")
    class AddUserBook {
        
        @Test
        @DisplayName("존재하는 책과 사용자로 등록 시 성공한다")
        void addUserBook_WithExistingBookAndUser_ShouldReturnUserBook() {
            // Given
            Long userId = 1L;
            Long bookId = 1L;
            
            User user = User.builder().id(userId).email("test@example.com").build();
            Book book = Book.builder().id(bookId).title("테스트 책").build();
            
            CreateUserBookRequest request = CreateUserBookRequest.builder()
                .userId(userId)
                .bookId(bookId)
                .status(ReadingStatus.NOT_STARTED)
                .build();
                
            UserBook expectedUserBook = UserBook.builder()
                .id(1L)
                .user(user)
                .book(book)
                .status(ReadingStatus.NOT_STARTED)
                .currentPage(0)
                .build();
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(userBookRepository.save(any(UserBook.class))).thenReturn(expectedUserBook);
            
            // When
            UserBook actualUserBook = bookService.addUserBook(request);
            
            // Then
            assertThat(actualUserBook).isNotNull();
            assertThat(actualUserBook.getUser()).isEqualTo(user);
            assertThat(actualUserBook.getBook()).isEqualTo(book);
            assertThat(actualUserBook.getStatus()).isEqualTo(ReadingStatus.NOT_STARTED);
        }
    }
}
```

#### 리포지토리 계층 테스트
```java
@DataJpaTest
@DisplayName("BookRepository 테스트")
class BookRepositoryTest {
    
    @Autowired private TestEntityManager entityManager;
    @Autowired private BookRepository bookRepository;
    
    @Test
    @DisplayName("제목으로 책 검색이 정상적으로 동작한다")
    void findByTitleContainingIgnoreCase_ShouldReturnMatchingBooks() {
        // Given
        Book book1 = Book.builder()
            .title("클린 코드")
            .author("로버트 C. 마틴")
            .build();
        Book book2 = Book.builder()
            .title("클린 아키텍처")
            .author("로버트 C. 마틴")
            .build();
        Book book3 = Book.builder()
            .title("이펙티브 자바")
            .author("조슈아 블로크")
            .build();
            
        entityManager.persistAndFlush(book1);
        entityManager.persistAndFlush(book2);
        entityManager.persistAndFlush(book3);
        
        // When
        Page<Book> result = bookRepository.findByTitleContainingIgnoreCase(
            "클린", PageRequest.of(0, 10));
        
        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(Book::getTitle)
            .containsExactly("클린 코드", "클린 아키텍처");
    }
    
    @Test
    @DisplayName("카테고리별 책 개수 조회가 정상적으로 동작한다")
    void countByCategory_ShouldReturnCorrectCount() {
        // Given
        Book techBook1 = Book.builder()
            .title("클린 코드")
            .category(BookCategory.TECHNOLOGY)
            .build();
        Book techBook2 = Book.builder()
            .title("이펙티브 자바")
            .category(BookCategory.TECHNOLOGY)
            .build();
        Book fictionBook = Book.builder()
            .title("1984")
            .category(BookCategory.FICTION)
            .build();
            
        entityManager.persistAndFlush(techBook1);
        entityManager.persistAndFlush(techBook2);
        entityManager.persistAndFlush(fictionBook);
        
        // When
        long techCount = bookRepository.countByCategory(BookCategory.TECHNOLOGY);
        long fictionCount = bookRepository.countByCategory(BookCategory.FICTION);
        
        // Then
        assertThat(techCount).isEqualTo(2);
        assertThat(fictionCount).isEqualTo(1);
    }
}
```

### 통합 테스트

#### API 통합 테스트
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Book API 통합 테스트")
class BookControllerIntegrationTest {
    
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    
    private User testUser;
    private String authToken;
    
    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
            User.builder()
                .email("test@example.com")
                .username("테스트사용자")
                .password("encodedPassword")
                .build()
        );
        
        // JWT 토큰 생성 (실제 구현에 따라 조정)
        authToken = generateTestToken(testUser);
    }
    
    @Test
    @DisplayName("책 목록 조회 API가 정상적으로 동작한다")
    void getBooks_ShouldReturnBookList() {
        // Given
        Book book1 = bookRepository.save(
            Book.builder()
                .title("클린 코드")
                .author("로버트 C. 마틴")
                .category(BookCategory.TECHNOLOGY)
                .build()
        );
        Book book2 = bookRepository.save(
            Book.builder()
                .title("1984")
                .author("조지 오웰")
                .category(BookCategory.FICTION)
                .build()
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<PagedResponse<BookResponse>> response = restTemplate.exchange(
            "/api/v1/books",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<PagedResponse<BookResponse>>() {}
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getContent())
            .extracting(BookResponse::getTitle)
            .containsExactly("클린 코드", "1984");
    }
    
    @Test
    @DisplayName("책 생성 API가 정상적으로 동작한다")
    void createBook_WithValidData_ShouldReturnCreatedBook() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
            .title("새로운 책")
            .author("테스트 저자")
            .publisher("테스트 출판사")
            .totalPages(300)
            .category(BookCategory.TECHNOLOGY)
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateBookRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<BookResponse> response = restTemplate.exchange(
            "/api/v1/books",
            HttpMethod.POST,
            entity,
            BookResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("새로운 책");
        assertThat(response.getBody().getAuthor()).isEqualTo("테스트 저자");
        
        // 데이터베이스에서 확인
        Optional<Book> savedBook = bookRepository.findById(response.getBody().getId());
        assertThat(savedBook).isPresent();
        assertThat(savedBook.get().getTitle()).isEqualTo("새로운 책");
    }
}
```

## AI 서비스 테스트

### 단위 테스트

#### 피드백 서비스 테스트
```python
import pytest
from unittest.mock import Mock, patch, AsyncMock
from services.feedback_service import FeedbackService
from models.feedback_models import FeedbackRequest, FeedbackResponse

class TestFeedbackService:
    
    @pytest.fixture
    def feedback_service(self):
        return FeedbackService()
    
    @pytest.fixture
    def mock_openai_service(self):
        with patch('services.feedback_service.openai_service') as mock:
            yield mock
    
    @pytest.mark.asyncio
    async def test_generate_feedback_success(self, feedback_service, mock_openai_service):
        """피드백 생성 성공 테스트"""
        # Given
        request = FeedbackRequest(
            note_content="클린 코드의 중요성에 대해 배웠다.",
            feedback_type="QUESTION"
        )
        
        mock_response = {
            "content": "클린 코드에 대해 더 구체적으로 어떤 부분이 인상깊었나요?",
            "feedback_type": "QUESTION"
        }
        
        mock_openai_service.generate_feedback.return_value = mock_response
        
        # When
        result = await feedback_service.generate_feedback(request)
        
        # Then
        assert isinstance(result, FeedbackResponse)
        assert result.content == mock_response["content"]
        assert result.feedback_type == "QUESTION"
        mock_openai_service.generate_feedback.assert_called_once_with(
            request.note_content, request.feedback_type
        )
    
    @pytest.mark.asyncio
    async def test_generate_feedback_empty_content_raises_error(self, feedback_service):
        """빈 내용으로 피드백 생성 시 에러 발생 테스트"""
        # Given
        request = FeedbackRequest(
            note_content="",
            feedback_type="QUESTION"
        )
        
        # When & Then
        with pytest.raises(ValueError, match="노트 내용이 비어있습니다"):
            await feedback_service.generate_feedback(request)
    
    @pytest.mark.asyncio
    async def test_generate_feedback_invalid_type_raises_error(self, feedback_service):
        """잘못된 피드백 타입으로 생성 시 에러 발생 테스트"""
        # Given
        request = FeedbackRequest(
            note_content="테스트 내용",
            feedback_type="INVALID_TYPE"
        )
        
        # When & Then
        with pytest.raises(ValueError, match="지원하지 않는 피드백 타입"):
            await feedback_service.generate_feedback(request)
    
    @pytest.mark.asyncio
    async def test_generate_feedback_with_rate_limiting(self, feedback_service, mock_openai_service):
        """레이트 리미팅 적용 테스트"""
        # Given
        request = FeedbackRequest(
            note_content="테스트 내용",
            feedback_type="QUESTION"
        )
        
        # 레이트 리미팅 모킹
        with patch('services.feedback_service.rate_limiter') as mock_limiter:
            mock_limiter.is_allowed.return_value = False
            
            # When & Then
            with pytest.raises(Exception, match="요청 한도를 초과했습니다"):
                await feedback_service.generate_feedback(request)
```

### 통합 테스트

#### API 엔드포인트 테스트
```python
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

class TestFeedbackAPI:
    
    def test_generate_feedback_endpoint_success(self):
        """피드백 생성 엔드포인트 성공 테스트"""
        # Given
        request_data = {
            "note_content": "객체지향 프로그래밍의 SOLID 원칙에 대해 학습했다.",
            "feedback_type": "QUESTION"
        }
        
        # When
        response = client.post("/feedback/generate", json=request_data)
        
        # Then
        assert response.status_code == 200
        data = response.json()
        assert "content" in data
        assert "feedback_type" in data
        assert data["feedback_type"] == "QUESTION"
        assert len(data["content"]) > 0
    
    def test_generate_feedback_endpoint_validation_error(self):
        """피드백 생성 엔드포인트 유효성 검사 에러 테스트"""
        # Given
        request_data = {
            "note_content": "",  # 빈 내용
            "feedback_type": "QUESTION"
        }
        
        # When
        response = client.post("/feedback/generate", json=request_data)
        
        # Then
        assert response.status_code == 422
        data = response.json()
        assert "detail" in data
    
    def test_health_check_endpoint(self):
        """헬스체크 엔드포인트 테스트"""
        # When
        response = client.get("/health")
        
        # Then
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
    
    @pytest.mark.asyncio
    async def test_feedback_caching(self):
        """피드백 캐싱 동작 테스트"""
        # Given
        request_data = {
            "note_content": "동일한 내용으로 캐싱 테스트",
            "feedback_type": "SUGGESTION"
        }
        
        # When - 첫 번째 요청
        response1 = client.post("/feedback/generate", json=request_data)
        
        # When - 두 번째 요청 (동일한 내용)
        response2 = client.post("/feedback/generate", json=request_data)
        
        # Then
        assert response1.status_code == 200
        assert response2.status_code == 200
        assert response1.json() == response2.json()  # 캐시된 결과 동일
```

## 프론트엔드 테스트

### 컴포넌트 테스트

#### React Native 컴포넌트 테스트
```typescript
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { jest } from '@jest/globals';
import BookCard from '../BookCard';
import { Book } from '../../types';

const mockBook: Book = {
  id: 1,
  title: '클린 코드',
  author: '로버트 C. 마틴',
  publisher: '인사이트',
  totalPages: 584,
  category: 'TECHNOLOGY',
  description: '깨끗한 코드 작성법',
  isbn: '9788966260959',
  publishedYear: 2013,
  createdAt: new Date(),
  updatedAt: new Date(),
};

describe('BookCard', () => {
  it('책 정보를 올바르게 렌더링한다', () => {
    // Given
    const onPress = jest.fn();
    
    // When
    const { getByText, getByTestId } = render(
      <BookCard book={mockBook} onPress={onPress} />
    );
    
    // Then
    expect(getByText('클린 코드')).toBeTruthy();
    expect(getByText('로버트 C. 마틴')).toBeTruthy();
    expect(getByText('인사이트')).toBeTruthy();
    expect(getByTestId('book-card')).toBeTruthy();
  });

  it('카드 터치 시 onPress 콜백이 호출된다', () => {
    // Given
    const onPress = jest.fn();
    
    // When
    const { getByTestId } = render(
      <BookCard book={mockBook} onPress={onPress} />
    );
    
    fireEvent.press(getByTestId('book-card'));
    
    // Then
    expect(onPress).toHaveBeenCalledWith(mockBook.id);
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('로딩 상태를 올바르게 표시한다', () => {
    // Given
    const onPress = jest.fn();
    
    // When
    const { getByTestId } = render(
      <BookCard book={mockBook} onPress={onPress} loading={true} />
    );
    
    // Then
    expect(getByTestId('loading-indicator')).toBeTruthy();
  });
});
```

#### 훅 테스트
```typescript
import { renderHook, act } from '@testing-library/react-hooks';
import { useBooks } from '../useBooks';
import { bookService } from '../../services/bookService';

// Mock 서비스
jest.mock('../../services/bookService');
const mockBookService = bookService as jest.Mocked<typeof bookService>;

describe('useBooks', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('초기 상태가 올바르게 설정된다', () => {
    // When
    const { result } = renderHook(() => useBooks());
    
    // Then
    expect(result.current.books).toEqual([]);
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('책 목록을 성공적으로 로드한다', async () => {
    // Given
    const mockBooks = [mockBook];
    mockBookService.getUserBooks.mockResolvedValue({
      success: true,
      data: { content: mockBooks, totalElements: 1 }
    });

    // When
    const { result } = renderHook(() => useBooks());
    
    await act(async () => {
      await result.current.loadBooks();
    });

    // Then
    expect(result.current.books).toEqual(mockBooks);
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('책 로드 실패 시 에러 상태를 설정한다', async () => {
    // Given
    const errorMessage = '네트워크 오류';
    mockBookService.getUserBooks.mockResolvedValue({
      success: false,
      message: errorMessage
    });

    // When
    const { result } = renderHook(() => useBooks());
    
    await act(async () => {
      await result.current.loadBooks();
    });

    // Then
    expect(result.current.books).toEqual([]);
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBe(errorMessage);
  });
});
```

### E2E 테스트

#### Detox E2E 테스트 (향후 구현)
```typescript
describe('BookReview App E2E', () => {
  beforeAll(async () => {
    await device.launchApp();
  });

  beforeEach(async () => {
    await device.reloadReactNative();
  });

  it('사용자가 책을 검색하고 상세 정보를 볼 수 있다', async () => {
    // 홈 화면에서 검색 탭으로 이동
    await element(by.id('search-tab')).tap();
    
    // 검색어 입력
    await element(by.id('search-input')).typeText('클린 코드');
    await element(by.id('search-button')).tap();
    
    // 검색 결과 확인
    await expect(element(by.text('클린 코드'))).toBeVisible();
    
    // 책 카드 터치
    await element(by.id('book-card-1')).tap();
    
    // 책 상세 화면 확인
    await expect(element(by.id('book-detail-screen'))).toBeVisible();
    await expect(element(by.text('로버트 C. 마틴'))).toBeVisible();
  });

  it('사용자가 독서 노트를 작성할 수 있다', async () => {
    // 내 책 탭으로 이동
    await element(by.id('my-books-tab')).tap();
    
    // 읽고 있는 책 선택
    await element(by.id('reading-book-1')).tap();
    
    // 노트 작성 버튼 터치
    await element(by.id('add-note-button')).tap();
    
    // 노트 내용 입력
    await element(by.id('note-content-input')).typeText('오늘 배운 내용입니다.');
    
    // 저장 버튼 터치
    await element(by.id('save-note-button')).tap();
    
    // 저장 완료 확인
    await expect(element(by.text('노트가 저장되었습니다.'))).toBeVisible();
  });
});
```

## 테스트 환경 설정

### 백엔드 테스트 환경

#### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  redis:
    host: localhost
    port: 6370  # 테스트용 포트
    
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### AI 서비스 테스트 환경

#### pytest.ini
```ini
[tool:pytest]
testpaths = tests
python_files = test_*.py
python_classes = Test*
python_functions = test_*
addopts = 
    -v
    --tb=short
    --cov=app
    --cov-report=html
    --cov-report=term-missing
    --cov-fail-under=80
asyncio_mode = auto
```

#### conftest.py
```python
import pytest
import asyncio
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock
from main import app

@pytest.fixture
def client():
    return TestClient(app)

@pytest.fixture
def mock_openai_service():
    return AsyncMock()

@pytest.fixture
def mock_redis():
    return AsyncMock()

@pytest.fixture
async def test_feedback_data():
    return {
        "note_content": "테스트 노트 내용",
        "feedback_type": "QUESTION"
    }
```

### 프론트엔드 테스트 환경

#### jest.config.js
```javascript
module.exports = {
  preset: 'react-native',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  testMatch: [
    '**/__tests__/**/*.ts?(x)',
    '**/?(*.)+(spec|test).ts?(x)'
  ],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  transform: {
    '^.+\\.(ts|tsx)$': 'ts-jest',
  },
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/types/**/*',
  ],
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70,
    },
  },
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@components/(.*)$': '<rootDir>/src/components/$1',
    '^@services/(.*)$': '<rootDir>/src/services/$1',
  },
};
```

## 테스트 데이터 관리

### 데이터베이스 테스트 데이터

#### TestDataBuilder 패턴
```java
public class BookTestDataBuilder {
    private String title = "기본 제목";
    private String author = "기본 저자";
    private BookCategory category = BookCategory.TECHNOLOGY;
    private Integer totalPages = 300;
    
    public static BookTestDataBuilder aBook() {
        return new BookTestDataBuilder();
    }
    
    public BookTestDataBuilder withTitle(String title) {
        this.title = title;
        return this;
    }
    
    public BookTestDataBuilder withAuthor(String author) {
        this.author = author;
        return this;
    }
    
    public BookTestDataBuilder withCategory(BookCategory category) {
        this.category = category;
        return this;
    }
    
    public Book build() {
        return Book.builder()
            .title(title)
            .author(author)
            .category(category)
            .totalPages(totalPages)
            .build();
    }
}

// 사용 예시
Book testBook = BookTestDataBuilder.aBook()
    .withTitle("클린 코드")
    .withAuthor("로버트 C. 마틴")
    .withCategory(BookCategory.TECHNOLOGY)
    .build();
```

### 모킹 전략

#### WireMock을 활용한 외부 API 모킹
```java
@ExtendWith(WireMockExtension.class)
class ExternalApiTest {
    
    @Test
    void externalBookApi_ShouldReturnBookInfo(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        
        wireMock.register(
            get(urlEqualTo("/api/books/9788966260959"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                            "title": "클린 코드",
                            "author": "로버트 C. 마틴",
                            "isbn": "9788966260959"
                        }
                        """))
        );
        
        // When & Then
        // 실제 API 호출 테스트
    }
}
```

## 성능 테스트

### JMeter 테스트 시나리오

#### API 성능 테스트
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="BookReview API Performance Test">
      <elementProp name="TestPlan.arguments" elementType="Arguments"/>
      <stringProp name="TestPlan.comments">책 리뷰 플랫폼 API 성능 테스트</stringProp>
    </TestPlan>
    
    <hashTree>
      <ThreadGroup testname="Book API Load Test">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
      </ThreadGroup>
      
      <hashTree>
        <HTTPSamplerProxy testname="GET Books API">
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/books</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
        
        <ResponseAssertion testname="Response Time Assertion">
          <stringProp name="Assertion.test_field">Assertion.response_time</stringProp>
          <stringProp name="Assertion.duration">1000</stringProp>
        </ResponseAssertion>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### 데이터베이스 성능 테스트

#### 쿼리 성능 테스트
```java
@Test
@DisplayName("대량 데이터 조회 성능 테스트")
void performanceTest_LargeDataQuery() {
    // Given - 대량 테스트 데이터 생성
    List<Book> books = IntStream.range(0, 10000)
        .mapToObj(i -> BookTestDataBuilder.aBook()
            .withTitle("테스트 책 " + i)
            .build())
        .collect(Collectors.toList());
    
    bookRepository.saveAll(books);
    
    // When
    long startTime = System.currentTimeMillis();
    
    Page<Book> result = bookRepository.findByTitleContainingIgnoreCase(
        "테스트", PageRequest.of(0, 50));
    
    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;
    
    // Then
    assertThat(result.getContent()).hasSize(50);
    assertThat(executionTime).isLessThan(1000); // 1초 이내 응답
}
```

## 테스트 자동화

### GitHub Actions 워크플로우

#### .github/workflows/test.yml
```yaml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: rootpassword
          MYSQL_DATABASE: bookreview_test
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Run backend tests
      run: |
        cd backend
        ./gradlew test --no-daemon
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Backend Test Results
        path: backend/build/test-results/test/*.xml
        reporter: java-junit

  ai-service-test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python 3.11
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
    
    - name: Install dependencies
      run: |
        cd ai-service
        python -m pip install --upgrade pip
        pip install -r requirements.txt
        pip install pytest pytest-cov
    
    - name: Run AI service tests
      run: |
        cd ai-service
        pytest --cov=app --cov-report=xml
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ai-service/coverage.xml
        flags: ai-service

  frontend-test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: mobile/BookReviewApp/package-lock.json
    
    - name: Install dependencies
      run: |
        cd mobile/BookReviewApp
        npm ci
    
    - name: Run frontend tests
      run: |
        cd mobile/BookReviewApp
        npm test -- --coverage --watchAll=false
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: mobile/BookReviewApp/coverage/lcov.info
        flags: frontend
```

### 테스트 결과 리포팅

#### SonarQube 연동
```yaml
- name: SonarQube Scan
  uses: sonarqube-quality-gate-action@master
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  with:
    scanMetadataReportFile: target/sonar/report-task.txt
```

## 테스트 메트릭 및 모니터링

### 커버리지 목표
- **백엔드**: 85% 이상
- **AI 서비스**: 80% 이상  
- **프론트엔드**: 75% 이상

### 성능 기준
- **API 응답 시간**: 95%의 요청이 1초 이내
- **데이터베이스 쿼리**: 평균 500ms 이내
- **AI 피드백 생성**: 평균 3초 이내

### 품질 게이트
- 모든 테스트 통과
- 코드 커버리지 기준 충족
- 정적 분석 도구 통과 (SonarQube)
- 보안 취약점 검사 통과

이러한 테스트 전략을 통해 안정적이고 신뢰할 수 있는 BookReview LLM Platform을 구축할 수 있습니다.