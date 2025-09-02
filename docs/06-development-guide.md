# 개발 가이드

## 목차
1. [개발 환경 설정](#개발-환경-설정)
2. [코드 스타일 가이드](#코드-스타일-가이드)
3. [Git 워크플로우](#git-워크플로우)
4. [테스트 가이드](#테스트-가이드)
5. [API 개발 가이드](#api-개발-가이드)
6. [프론트엔드 개발 가이드](#프론트엔드-개발-가이드)
7. [배포 가이드](#배포-가이드)

## 개발 환경 설정

### 필수 도구
- **JDK**: OpenJDK 21 (Eclipse Temurin)
- **Node.js**: 18.x 이상
- **Python**: 3.11.x (Anaconda 권장)
- **Docker**: 최신 버전
- **IDE**: IntelliJ IDEA Ultimate (권장)

### 개발 환경 초기화

```bash
# 1. 저장소 클론
git clone <repository-url>
cd BookReview-LLM-Platform

# 2. Docker 환경 실행
docker-compose -f docker-compose.dev.yml up -d

# 3. 백엔드 의존성 설치 및 실행
cd backend
./gradlew build
./gradlew bootRun --args='--spring.profiles.active=dev'

# 4. AI 서비스 실행
cd ../ai-service
conda create -n bookreview python=3.11
conda activate bookreview
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 5. 모바일 앱 실행
cd ../mobile/BookReviewApp
npm install
npm start
```

## 코드 스타일 가이드

### Java (Spring Boot)
- **표준**: Google Java Style Guide
- **포맷터**: IntelliJ 기본 설정
- **네이밍**: 
  - 클래스: PascalCase
  - 메서드/변수: camelCase
  - 상수: UPPER_SNAKE_CASE
  - 패키지: lowercase

```java
// 좋은 예
@Service
public class BookService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    
    public UserBook createUserBook(CreateUserBookRequest request) {
        // 구현
    }
}
```

### Python (FastAPI)
- **표준**: PEP 8
- **포맷터**: Black
- **네이밍**:
  - 클래스: PascalCase
  - 함수/변수: snake_case
  - 상수: UPPER_SNAKE_CASE

```python
# 좋은 예
class FeedbackService:
    MAX_RETRY_COUNT = 3
    
    async def generate_feedback(self, note_content: str) -> FeedbackResponse:
        # 구현
        pass
```

### TypeScript (React Native)
- **표준**: Airbnb TypeScript Style Guide
- **포맷터**: Prettier
- **네이밍**:
  - 컴포넌트: PascalCase
  - 함수/변수: camelCase
  - 인터페이스: PascalCase (I 접두사 없이)

```typescript
// 좋은 예
interface BookProps {
  book: Book;
  onPress: (bookId: number) => void;
}

const BookCard: React.FC<BookProps> = ({ book, onPress }) => {
  // 구현
};
```

## Git 워크플로우

### 브랜치 전략
- **main**: 운영 배포용 브랜치
- **develop**: 개발 통합 브랜치
- **feature/기능명**: 기능 개발 브랜치
- **hotfix/이슈명**: 긴급 수정 브랜치

### 커밋 메시지 컨벤션
```
type(scope): subject

body

footer
```

**타입**:
- `feat`: 새로운 기능
- `fix`: 버그 수정
- `docs`: 문서 변경
- `style`: 포맷팅, 세미콜론 누락 등
- `refactor`: 코드 리팩토링
- `test`: 테스트 추가
- `chore`: 빌드, 패키지 매니저 설정 등

**예시**:
```
feat(auth): add Google OAuth login

- Google OAuth 2.0 인증 구현
- JWT 토큰 발급 및 검증 로직 추가
- 사용자 정보 자동 동기화

Closes #123
```

## 테스트 가이드

### 백엔드 테스트 (Spring Boot)

#### 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock
    private BookRepository bookRepository;
    
    @InjectMocks
    private BookService bookService;
    
    @Test
    @DisplayName("책 생성 시 정상적으로 저장되어야 한다")
    void createBook_ShouldReturnSavedBook() {
        // Given
        CreateBookRequest request = CreateBookRequest.builder()
            .title("테스트 책")
            .author("테스트 저자")
            .build();
        
        Book savedBook = Book.builder()
            .id(1L)
            .title("테스트 책")
            .author("테스트 저자")
            .build();
            
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        
        // When
        Book result = bookService.createBook(request);
        
        // Then
        assertThat(result.getTitle()).isEqualTo("테스트 책");
        assertThat(result.getAuthor()).isEqualTo("테스트 저자");
    }
}
```

#### 통합 테스트
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class BookControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("책 목록 조회 API 테스트")
    void getBooks_ShouldReturnBookList() {
        // Given
        String url = "/api/v1/books";
        
        // When
        ResponseEntity<PagedResponse<BookResponse>> response = 
            restTemplate.exchange(url, HttpMethod.GET, null, 
                new ParameterizedTypeReference<PagedResponse<BookResponse>>() {});
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isNotEmpty();
    }
}
```

### AI 서비스 테스트 (FastAPI)

```python
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

class TestFeedbackEndpoint:
    def test_generate_feedback_success(self):
        """피드백 생성 성공 테스트"""
        # Given
        request_data = {
            "note_content": "클린 코드에 대해 배웠다.",
            "feedback_type": "QUESTION"
        }
        
        # When
        response = client.post("/feedback/generate", json=request_data)
        
        # Then
        assert response.status_code == 200
        data = response.json()
        assert "content" in data
        assert "feedback_type" in data
        
    def test_generate_feedback_empty_content(self):
        """빈 내용으로 피드백 생성 시 실패 테스트"""
        # Given
        request_data = {
            "note_content": "",
            "feedback_type": "QUESTION"
        }
        
        # When
        response = client.post("/feedback/generate", json=request_data)
        
        # Then
        assert response.status_code == 422
```

### 프론트엔드 테스트 (React Native)

```typescript
import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import BookCard from '../BookCard';

describe('BookCard', () => {
  const mockBook = {
    id: 1,
    title: '테스트 책',
    author: '테스트 저자',
    // ... 기타 속성
  };

  it('책 정보를 올바르게 표시해야 한다', () => {
    // Given
    const onPress = jest.fn();
    
    // When
    const { getByText } = render(
      <BookCard book={mockBook} onPress={onPress} />
    );
    
    // Then
    expect(getByText('테스트 책')).toBeTruthy();
    expect(getByText('테스트 저자')).toBeTruthy();
  });

  it('카드 클릭 시 onPress 콜백이 호출되어야 한다', () => {
    // Given
    const onPress = jest.fn();
    
    // When
    const { getByTestId } = render(
      <BookCard book={mockBook} onPress={onPress} />
    );
    
    fireEvent.press(getByTestId('book-card'));
    
    // Then
    expect(onPress).toHaveBeenCalledWith(1);
  });
});
```

## API 개발 가이드

### REST API 설계 원칙

1. **RESTful URL 구조**
```
GET    /api/v1/books                    # 책 목록 조회
GET    /api/v1/books/{id}               # 책 상세 조회
POST   /api/v1/books                    # 책 생성
PUT    /api/v1/books/{id}               # 책 전체 수정
PATCH  /api/v1/books/{id}               # 책 부분 수정
DELETE /api/v1/books/{id}               # 책 삭제

GET    /api/v1/user-books               # 사용자 책 목록
POST   /api/v1/user-books               # 사용자 책 등록
```

2. **응답 형식 표준화**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "클린 코드",
    "author": "로버트 C. 마틴"
  },
  "message": "성공적으로 처리되었습니다."
}

// 에러 응답
{
  "success": false,
  "errors": [
    {
      "field": "title",
      "message": "제목은 필수입니다."
    }
  ],
  "message": "입력 데이터가 올바르지 않습니다."
}
```

3. **페이징 처리**
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 100,
  "totalPages": 10,
  "last": false,
  "first": true
}
```

### API 문서화
- **도구**: OpenAPI 3.0 (Swagger)
- **자동 생성**: Spring Boot (`@Operation`, `@ApiResponse` 어노테이션)
- **접근 URL**: `http://localhost:8080/swagger-ui.html`

## 프론트엔드 개발 가이드

### 컴포넌트 구조
```
src/
├── components/
│   ├── common/          # 공통 컴포넌트
│   │   ├── Button/
│   │   ├── Input/
│   │   └── Loading/
│   └── feature/         # 기능별 컴포넌트
│       ├── BookCard/
│       ├── NoteEditor/
│       └── FeedbackList/
├── screens/             # 화면 컴포넌트
├── hooks/              # 커스텀 훅
└── utils/              # 유틸리티
```

### 상태 관리
- **전역 상태**: Context API + useReducer
- **서버 상태**: React Query (향후 도입 예정)
- **로컬 상태**: useState, useReducer

```typescript
// 컨텍스트 예시
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

## 배포 가이드

### 개발 환경 배포
```bash
# Docker Compose 사용
docker-compose -f docker-compose.dev.yml up -d
```

### 운영 환경 배포
```bash
# 1. 빌드
docker-compose build

# 2. 운영 환경 실행
docker-compose -f docker-compose.yml up -d

# 3. 헬스체크 확인
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health
```

### CI/CD 파이프라인 (향후 구현)
1. **코드 품질 검사**: SonarQube
2. **테스트 실행**: JUnit, Jest, pytest
3. **빌드**: Docker 이미지 생성
4. **배포**: Kubernetes 또는 Docker Swarm

## 성능 최적화 가이드

### 백엔드 최적화
- **데이터베이스 인덱스**: 자주 조회되는 컬럼에 인덱스 생성
- **JPA 최적화**: N+1 문제 해결 (Fetch Join, Entity Graph)
- **캐싱**: Redis를 활용한 조회 성능 향상

### AI 서비스 최적화
- **응답 캐싱**: 동일한 요청에 대한 캐시 처리
- **비동기 처리**: 긴 작업에 대한 비동기 처리
- **배치 처리**: 여러 요청을 묶어서 처리

### 모바일 앱 최적화
- **이미지 최적화**: WebP 포맷 사용
- **번들 크기 최적화**: 코드 스플리팅
- **렌더링 최적화**: FlatList, 메모이제이션 활용

## 모니터링 및 로깅

### 로그 수준
- **ERROR**: 시스템 오류
- **WARN**: 주의가 필요한 상황
- **INFO**: 일반적인 정보
- **DEBUG**: 디버깅 정보

### 메트릭 수집
- **애플리케이션 메트릭**: Micrometer + Prometheus
- **시스템 메트릭**: cAdvisor
- **로그 수집**: ELK Stack (향후 도입)

## 보안 가이드

### 인증 및 인가
- **JWT 토큰**: 액세스 토큰 + 리프레시 토큰
- **OAuth 2.0**: Google 로그인 지원
- **RBAC**: 역할 기반 접근 제어

### 데이터 보안
- **개인정보 암호화**: AES-256
- **비밀번호 해싱**: BCrypt
- **SQL Injection 방지**: JPA 사용
- **XSS 방지**: 입력 값 검증 및 이스케이프

## 문제 해결 가이드

### 자주 발생하는 문제

#### 1. 데이터베이스 연결 오류
```bash
# MySQL 컨테이너 상태 확인
docker-compose ps mysql

# 로그 확인
docker-compose logs mysql

# 재시작
docker-compose restart mysql
```

#### 2. AI 서비스 연결 오류
```bash
# Python 가상환경 확인
conda activate bookreview

# 의존성 재설치
pip install -r requirements.txt

# 서비스 재시작
uvicorn main:app --reload
```

#### 3. React Native 빌드 오류
```bash
# 캐시 정리
npx react-native start --reset-cache

# node_modules 재설치
rm -rf node_modules && npm install

# Metro 서버 재시작
npm start
```

### 지원 및 문의
- **Issue 트래킹**: GitHub Issues
- **코드 리뷰**: Pull Request
- **문서 업데이트**: docs/ 디렉토리