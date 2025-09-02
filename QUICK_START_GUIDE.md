# 🚀 BookReview-LLM-Platform 빠른 시작 가이드

## 📋 시스템 요구사항

### 필수 도구
- ☑️ **Java 21** (Eclipse Temurin 권장)
- ☑️ **Node.js 18+** & npm
- ☑️ **Python 3.11**
- ☑️ **Docker Desktop**
- ☑️ **Git**

### 선택 도구
- **IDE**: IntelliJ IDEA, VS Code
- **Database Client**: DBeaver, MySQL Workbench
- **API Testing**: Postman, Insomnia

## 🛠️ 로컬 개발 환경 설정

### 1. 프로젝트 클론 및 이동
```bash
git clone <repository-url>
cd BookReview-LLM-Platform
```

### 2. 데이터베이스 및 Redis 시작
```bash
# Docker Compose로 MySQL과 Redis 시작
docker-compose -f docker-compose.dev.yml up mysql redis -d

# 컨테이너 상태 확인
docker ps
```

### 3. AI Service 시작
```bash
# AI 서비스 디렉토리 이동
cd ai-service

# Python 의존성 설치
pip install -r requirements.txt

# 환경변수 설정 (Windows)
set ENVIRONMENT=development
set REDIS_HOST=localhost
set REDIS_PORT=6379
set OPENAI_API_KEY=your-api-key-here

# AI 서비스 시작
python main.py
```
**✅ 실행 확인**: http://localhost:8000/health

### 4. Spring Boot 백엔드 시작
```bash
# 백엔드 디렉토리 이동
cd backend

# JAR 파일로 실행
java -jar build/libs/bookreview-backend-1.0.0.jar --spring.profiles.active=dev

# 또는 Gradle로 개발 모드 실행
./gradlew bootRun
```
**✅ 실행 확인**: http://localhost:8080

### 5. React Native 모바일 앱 설정
```bash
# 모바일 앱 디렉토리 이동
cd mobile/BookReviewApp

# Node 의존성 설치
npm install

# 개발 서버 시작
npm start
```

## 🔧 주요 설정 파일

### Backend 설정
**파일**: `backend/src/main/resources/application-dev.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/bookreview
    username: bookreview_user
    password: bookreview_password
  
jwt:
  secret: dev-jwt-secret-key-for-development
  expiration: 3600
```

### AI Service 설정
**파일**: `ai-service/app/core/config.py`
```python
REDIS_HOST = "localhost"
REDIS_PORT = 6379
OPENAI_API_KEY = "your-api-key"
OPENAI_MODEL = "gpt-4"
```

### Docker 개발 환경
**파일**: `docker-compose.dev.yml`
```yaml
# MySQL: localhost:3307
# Redis: localhost:6379
# Backend: localhost:8080
# AI Service: localhost:8000
```

## 📚 API 엔드포인트

### 인증 API
```
POST /api/v1/auth/register  # 회원가입
POST /api/v1/auth/login     # 로그인  
POST /api/v1/auth/refresh   # 토큰 갱신
```

### 책 관리 API
```
GET    /api/v1/books        # 책 목록 조회
POST   /api/v1/books        # 책 등록
PUT    /api/v1/books/{id}   # 책 정보 수정
DELETE /api/v1/books/{id}   # 책 삭제
```

### 독서 노트 API
```
GET    /api/v1/notes        # 노트 목록 조회
POST   /api/v1/notes        # 노트 작성
PUT    /api/v1/notes/{id}   # 노트 수정
DELETE /api/v1/notes/{id}   # 노트 삭제
```

### AI 피드백 API
```
POST   /api/v1/ai/feedback  # AI 피드백 생성
GET    /api/v1/ai/feedback/{noteId}  # 피드백 조회
```

### 통계 API
```
GET    /api/v1/statistics/dashboard    # 대시보드 통계
GET    /api/v1/statistics/reading      # 독서 통계
GET    /api/v1/statistics/goals        # 목표 달성 통계
```

## 🧪 테스트 실행

### 백엔드 테스트
```bash
cd backend
./gradlew test
./gradlew jacocoTestReport  # 코드 커버리지
```

### AI 서비스 테스트
```bash
cd ai-service
pytest
pytest --cov  # 코드 커버리지
```

### API 테스트 (curl 예제)
```bash
# 헬스체크
curl http://localhost:8080/actuator/health
curl http://localhost:8000/health

# 회원가입
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","username":"테스트사용자"}'
```

## 📊 모니터링 및 디버깅

### 로그 확인
```bash
# Spring Boot 로그
tail -f backend/logs/application.log

# AI Service 로그  
# 콘솔에서 직접 확인 가능
```

### 데이터베이스 접속
```bash
# MySQL 직접 접속
mysql -h localhost -P 3307 -u bookreview_user -p bookreview

# 또는 Docker 컨테이너로 접속
docker exec -it bookreview-mysql-dev mysql -u bookreview_user -p bookreview
```

### Redis 모니터링
```bash
# Redis CLI 접속
redis-cli -h localhost -p 6379

# 키 목록 확인
KEYS *
```

## 🚨 일반적인 문제 해결

### 1. 포트 충돌
```bash
# 포트 사용 확인
netstat -ano | findstr :8080
netstat -ano | findstr :8000

# 프로세스 종료
taskkill /PID <PID번호> /F
```

### 2. 데이터베이스 연결 실패
```bash
# Docker 컨테이너 재시작
docker-compose -f docker-compose.dev.yml restart mysql

# 로그 확인
docker logs bookreview-mysql-dev
```

### 3. AI Service 오류
```bash
# OpenAI API 키 확인
echo $OPENAI_API_KEY

# Redis 연결 확인
redis-cli ping
```

### 4. 빌드 실패
```bash
# Gradle 캐시 정리
cd backend
./gradlew clean build

# Node 모듈 재설치
cd mobile/BookReviewApp
rm -rf node_modules package-lock.json
npm install
```

## 🌟 개발 팁

### 1. 핫 리로딩 활용
- **Backend**: Spring Boot DevTools로 자동 재시작
- **AI Service**: `--reload` 옵션으로 파일 변경 감지
- **Mobile**: Metro bundler로 실시간 업데이트

### 2. 코드 품질 유지
```bash
# Java 코드 포맷팅
./gradlew spotlessApply

# Python 코드 포맷팅
black ai-service/
isort ai-service/

# TypeScript 코드 검사
npm run lint
```

### 3. 성능 최적화
- **Database**: 쿼리 실행 계획 확인
- **Redis**: 캐시 히트율 모니터링  
- **API**: 응답 시간 측정

---

## 📞 지원 및 문의

**개발 환경 구성 문제**나 **기술적 질문**이 있으시면:
1. 🔍 **로그 파일** 먼저 확인
2. 📚 **API 문서** 참조
3. 🐛 **GitHub Issues** 등록

**Happy Coding!** 🎉