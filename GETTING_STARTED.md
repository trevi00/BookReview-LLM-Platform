# 🚀 BookReview-LLM-Platform 시작하기

## 📋 시작하기 전 체크리스트

### ✅ 필수 소프트웨어
- [ ] Java 21 (OpenJDK 또는 Oracle JDK)
- [ ] Docker Desktop
- [ ] IntelliJ IDEA (또는 다른 Java IDE)
- [ ] Anaconda 또는 Miniconda
- [ ] Git

### 🔑 환경 설정
- [ ] OpenAI API 키 발급 및 설정
- [ ] Docker Desktop 실행
- [ ] Git 프로젝트 클론

## 🚀 빠른 시작 가이드

### 1단계: 보안 설정 ⚠️
```bash
# 새로운 OpenAI API 키 발급 후
# ai-service\.env 파일 수정
OPENAI_API_KEY=sk-proj-your-new-api-key-here
```

### 2단계: 시스템 시작
```batch
# 전체 시스템 시작
start_system.bat
```

### 3단계: 개별 서비스 시작

#### A. 인프라 서비스 (Docker)
```bash
docker-compose up -d mysql redis
```

#### B. 백엔드 서비스 (IntelliJ)
1. IntelliJ IDEA에서 프로젝트 열기
2. `backend/src/main/java/com/bookreview/BookreviewApplication.java` 실행
3. 브라우저에서 확인: http://localhost:8080/swagger-ui.html

#### C. AI 서비스 (Anaconda)
```bash
conda activate bookreview-ai
cd ai-service
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### 4단계: 시스템 테스트
```bash
# 기본 테스트 실행
python test_system.py

# API 키 테스트
python test_api_key.py
```

## 🌐 서비스 접속 정보

| 서비스 | URL | 설명 |
|--------|-----|------|
| 백엔드 API | http://localhost:8080 | Spring Boot REST API |
| API 문서 | http://localhost:8080/swagger-ui.html | Swagger UI |
| AI 서비스 | http://localhost:8001 | FastAPI AI 서비스 |
| AI API 문서 | http://localhost:8001/docs | FastAPI 자동 문서 |
| MySQL | localhost:3306 | 데이터베이스 |
| Redis | localhost:6379 | 캐시 서버 |

## 📊 주요 기능 테스트

### 1. 사용자 인증
```bash
# 회원가입
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "TestPassword123!"
  }'

# 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123!"
  }'
```

### 2. AI 피드백 테스트
```bash
# AI 서비스 헬스체크
curl http://localhost:8001/health

# 시스템 분석
curl http://localhost:8001/api/v1/analytics/system/health
```

### 3. 데이터베이스 확인
```bash
# Actuator 헬스체크
curl http://localhost:8080/actuator/health
```

## 🛠️ 개발 워크플로

### 백엔드 개발
1. IntelliJ에서 Java 코드 수정
2. 자동 재시작 (Spring Boot DevTools)
3. Swagger UI에서 API 테스트

### AI 서비스 개발
1. Python 코드 수정
2. FastAPI 자동 리로드 (`--reload` 옵션)
3. `/docs`에서 API 테스트

### 데이터베이스 관리
```sql
-- MySQL 접속
mysql -h localhost -u root -p

-- 데이터베이스 확인
SHOW DATABASES;
USE bookreview_db;
SHOW TABLES;
```

## 🚨 문제 해결

### 포트 충돌
```bash
# 포트 사용 확인
netstat -an | findstr "8080"
netstat -an | findstr "8001"

# 프로세스 종료
taskkill /f /pid [PID]
```

### Docker 문제
```bash
# Docker 상태 확인
docker ps
docker-compose ps

# 컨테이너 재시작
docker-compose restart mysql
docker-compose restart redis
```

### 로그 확인
- **백엔드**: IntelliJ 콘솔 또는 `logs/spring.log`
- **AI 서비스**: 터미널 출력 또는 `logs/fastapi.log`
- **Docker**: `docker-compose logs [service-name]`

## 📚 추가 리소스

- [API 문서](http://localhost:8080/swagger-ui.html)
- [AI 서비스 문서](http://localhost:8001/docs)
- [보안 가이드](SECURITY.md)
- [Docker Compose 설정](docker-compose.yml)

## 🎯 다음 단계

1. ✅ 시스템 실행 및 기본 테스트
2. 🧪 통합 테스트 작성 및 실행
3. 📱 React Native 프론트엔드 개발
4. 🚀 배포 준비

---

**도움이 필요하시면 언제든 문의해주세요!** 🤝