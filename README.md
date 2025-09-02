# 📚 BookReview-LLM-Platform

> **AI 피드백과 함께하는 스마트 독서 플랫폼**

[![Development Status](https://img.shields.io/badge/Status-✅%20완료-brightgreen)](./DEVELOPMENT_COMPLETION_REPORT.md)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)](https://spring.io/projects/spring-boot)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.104.1-blue)](https://fastapi.tiangolo.com/)
[![React Native](https://img.shields.io/badge/React%20Native-0.75.3-purple)](https://reactnative.dev/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**BookReview-LLM-Platform**은 AI 기술을 활용한 차세대 독서 플랫폼입니다. 개인화된 AI 피드백, 체계적인 독서 관리, 그리고 데이터 기반 독서 분석을 통해 더 깊고 의미있는 독서 경험을 제공합니다.

## 🚀 프로젝트 개요

### 핵심 기능
- 📚 책 등록 및 목차 관리
- ✍️ 목차별 독서 기록 작성
- 🤖 AI 기반 개인화된 피드백
- 📊 독서 진행률 및 통계 대시보드
- 🔐 Google OAuth 및 일반 로그인 지원

### 기술 스택
- **백엔드**: Spring Boot 3.5.3 (Java 21)
- **AI 서비스**: FastAPI (Python 3.11)
- **프론트엔드**: React Native (웹 + 모바일)
- **데이터베이스**: MySQL 8.0 (Docker)
- **캐시**: Redis 7.2
- **컨테이너**: Docker & Docker Compose

## 📁 프로젝트 구조

```
BookReview-LLM-Platform/
├── docs/                          # 프로젝트 문서
│   ├── 01-requirements.md         # 요구사항 분석
│   ├── 02-conceptual-design.md    # 개념적 설계 (ERD)
│   ├── 03-logical-design.md       # 논리적 설계 (API)
│   ├── 04-physical-design.md      # 물리적 설계 (DB)
│   └── 05-tech-stack-versions.md  # 기술 스택 버전
├── backend/                       # Spring Boot 백엔드
│   ├── src/
│   ├── build.gradle
│   └── Dockerfile
├── ai-service/                    # FastAPI AI 서비스
│   ├── app/
│   ├── requirements.txt
│   └── Dockerfile
├── frontend/                      # React Native 프론트엔드
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── database/                      # 데이터베이스 설정
│   ├── init/
│   └── conf/
├── docker-compose.yml             # 전체 환경 설정
└── README.md
```

## 🛠 개발 환경 설정

### 1. 필수 소프트웨어
- **Java**: OpenJDK 21
- **Node.js**: 18.18.2 LTS
- **Python**: 3.11 (Anaconda 권장)
- **Docker**: 24.0.7+
- **Docker Compose**: 2.21.0+

### 2. 프로젝트 실행

```bash
# 프로젝트 클론 및 이동
git clone <repository-url>
cd BookReview-LLM-Platform

# 환경 변수 설정
cp .env.example .env
# .env 파일을 편집하여 필요한 설정값 입력

# 전체 서비스 실행
docker-compose up -d

# 개별 서비스 실행 (개발 모드)
cd backend && ./gradlew bootRun
cd ai-service && uvicorn main:app --reload
cd frontend && npm start
```

### 3. 서비스 접속
- **백엔드 API**: http://localhost:8080
- **AI 서비스**: http://localhost:8000
- **프론트엔드**: http://localhost:3000
- **데이터베이스**: localhost:3306
- **Redis**: localhost:6379

## 🧪 테스트

### 단위 테스트
```bash
# 백엔드 테스트
cd backend && ./gradlew test

# AI 서비스 테스트
cd ai-service && pytest

# 프론트엔드 테스트
cd frontend && npm test
```

### 통합 테스트
```bash
# 전체 테스트 실행
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

## 📝 개발 가이드

### TDD (Test-Driven Development)
1. **Red**: 실패하는 테스트 작성
2. **Green**: 테스트를 통과하는 최소한의 코드 작성
3. **Refactor**: 코드 개선 및 최적화

### 코드 스타일
- **Java**: Google Java Style Guide
- **Python**: PEP 8 + Black
- **TypeScript**: ESLint + Prettier

### 커밋 컨벤션
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 추가/수정
chore: 빌드 설정 등
```

## 🔧 주요 설정

### 환경 변수
```env
# 데이터베이스
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bookreview
DB_USERNAME=bookreview_user
DB_PASSWORD=bookreview_password

# JWT 설정
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# OpenAI API
OPENAI_API_KEY=your-openai-api-key
```

## 📊 API 문서

### 자동 생성 문서
- **백엔드**: http://localhost:8080/swagger-ui.html
- **AI 서비스**: http://localhost:8000/docs

### 주요 엔드포인트
```
POST   /api/v1/auth/login          # 로그인
POST   /api/v1/auth/register       # 회원가입
GET    /api/v1/books               # 책 목록 조회
POST   /api/v1/books               # 책 등록
POST   /api/v1/notes               # 독서 기록 작성
POST   /api/v1/ai/feedback         # AI 피드백 생성
```

## 🚦 프로젝트 상태

### ✅ **개발 완료** (2025.07.22 기준)

| 구성요소 | 상태 | 완료도 |
|----------|------|---------|
| **백엔드 API** | ✅ 완료 | 100% |
| **AI 서비스** | ✅ 완료 | 100% |  
| **데이터베이스** | ✅ 완료 | 100% |
| **모바일 앱 기반** | ✅ 완료 | 100% |
| **Docker 환경** | ✅ 완료 | 100% |
| **문서화** | ✅ 완료 | 100% |

### 🏆 **주요 성과**
- ✅ **100개 컴파일 에러 → 0개** (100% 해결)  
- ✅ **Enterprise급 보안** (JWT + Spring Security)
- ✅ **고성능 AI 통합** (GPT-4 + Redis 캐싱)
- ✅ **현대적 아키텍처** (DDD + Clean Architecture)
- ✅ **프로덕션 준비 완료** (Docker + CI/CD Ready)

## 🤝 기여 가이드

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 팀

- **Backend Developer**: Spring Boot, MySQL, Redis
- **AI Engineer**: FastAPI, OpenAI API, LangChain
- **Frontend Developer**: React Native, TypeScript
- **DevOps Engineer**: Docker, CI/CD, Monitoring

## 📞 문의

프로젝트에 관한 문의사항이 있으시면 Issues를 통해 연락해 주세요.

---

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!