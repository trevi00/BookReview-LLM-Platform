# BookReview LLM Platform 문서

AI 피드백과 함께하는 스마트 독서 플랫폼의 종합 문서입니다.

## 📚 문서 구조

### 1. [요구사항 분석](01-requirements.md)
- 프로젝트 개요 및 목표
- 기능 요구사항 (23개 주요 기능)
- 비기능 요구사항 (성능, 보안, 확장성)
- 사용자 스토리 및 시나리오
- MVP 정의 및 버전별 로드맵

### 2. [개념적 설계](02-conceptual-design.md)
- 도메인 모델링
- ERD (Entity Relationship Diagram)
- 핵심 엔티티 관계 정의
- 비즈니스 규칙 및 제약사항

### 3. [논리적 설계](03-logical-design.md)
- 시스템 아키텍처 (마이크로서비스)
- API 설계 및 엔드포인트 정의
- 서비스 간 통신 방식
- 보안 아키텍처 (JWT, OAuth2)

### 4. [물리적 설계](04-physical-design.md)
- 데이터베이스 스키마 (MySQL)
- 인덱스 및 성능 최적화
- 파일 시스템 구조
- 배포 아키텍처

### 5. [기술 스택](05-tech-stack-versions.md)
- 백엔드: Spring Boot 3.5.3 (Java 21)
- AI 서비스: FastAPI (Python 3.11)
- 프론트엔드: React Native 0.75.3
- 데이터베이스: MySQL 8.0, Redis 7.2
- 개발도구 및 버전 정보

### 6. [개발 가이드](06-development-guide.md)
- 개발 환경 설정
- 코딩 컨벤션 및 스타일 가이드
- Git 워크플로우
- API 개발 가이드
- 성능 최적화 방법

### 7. [테스트 전략](07-testing-strategy.md)
- TDD 개발 방법론
- 단위/통합/E2E 테스트 전략
- 테스트 자동화 설정
- 성능 테스트 방법
- 품질 게이트 정의

### 8. [배포 가이드](08-deployment-guide.md)
- Docker 컨테이너 배포
- 클라우드 배포 (AWS, Kubernetes)
- CI/CD 파이프라인
- 모니터링 및 로깅
- 백업 및 복구 전략

## 🚀 빠른 시작

### 전체 시스템 실행
```bash
# 저장소 클론
git clone <repository-url>
cd BookReview-LLM-Platform

# 개발 환경 실행
docker-compose -f docker-compose.dev.yml up -d

# 백엔드 실행
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'

# AI 서비스 실행
cd ai-service && uvicorn main:app --reload

# 모바일 앱 실행
cd mobile/BookReviewApp && npm start
```

### 주요 엔드포인트
- **백엔드 API**: http://localhost:8080/api/v1
- **AI 서비스**: http://localhost:8000
- **API 문서**: http://localhost:8080/swagger-ui.html
- **Grafana 모니터링**: http://localhost:3000

## 🏗️ 프로젝트 구조

```
BookReview-LLM-Platform/
├── docs/                           # 📋 프로젝트 문서
│   ├── 01-requirements.md         # 요구사항 분석
│   ├── 02-conceptual-design.md    # 개념적 설계
│   ├── 03-logical-design.md       # 논리적 설계
│   ├── 04-physical-design.md      # 물리적 설계
│   ├── 05-tech-stack-versions.md  # 기술 스택
│   ├── 06-development-guide.md    # 개발 가이드
│   ├── 07-testing-strategy.md     # 테스트 전략
│   └── 08-deployment-guide.md     # 배포 가이드
├── backend/                        # 🔧 Spring Boot 백엔드
│   ├── src/main/java/             # Java 소스 코드
│   ├── src/main/resources/        # 설정 파일
│   ├── src/test/                  # 테스트 코드
│   ├── build.gradle               # Gradle 빌드 설정
│   └── Dockerfile                 # Docker 빌드 파일
├── ai-service/                     # 🤖 FastAPI AI 서비스
│   ├── app/                       # Python 소스 코드
│   ├── tests/                     # 테스트 코드
│   ├── requirements.txt           # Python 의존성
│   └── Dockerfile                 # Docker 빌드 파일
├── mobile/                         # 📱 React Native 앱
│   └── BookReviewApp/             # React Native 프로젝트
│       ├── src/                   # TypeScript 소스 코드
│       ├── android/               # Android 설정
│       ├── ios/                   # iOS 설정
│       └── package.json           # npm 설정
├── database/                       # 🗄️ 데이터베이스 스크립트
│   ├── init/                      # 초기화 스크립트
│   └── migrations/                # 마이그레이션 스크립트
├── docker-compose.yml             # 🐳 운영 환경 Docker 설정
├── docker-compose.dev.yml         # 🛠️ 개발 환경 Docker 설정
└── README.md                      # 📖 프로젝트 개요
```

## 🔧 핵심 기능

### 📚 독서 관리
- 책 등록 및 검색
- 챕터별 독서 계획 수립
- 독서 진도 추적
- 독서 세션 기록

### 📝 독서 노트
- 챕터별 노트 작성
- 다양한 노트 유형 (요약, 질문, 감상, 학습, 인용)
- 페이지 번호 기반 정확한 위치 기록
- 공개/비공개 설정

### 🤖 AI 피드백
- OpenAI GPT 기반 개인화된 피드백
- 노트 유형별 맞춤 피드백
- 피드백 유용성 평가
- 응답 캐싱을 통한 성능 최적화

### 📊 독서 통계
- 독서 목표 설정 및 진도 관리
- 월별/연도별 독서 통계
- 카테고리별 독서 분포
- 개인 독서 패턴 분석

### 🔐 사용자 관리
- 로컬 계정 및 Google OAuth 로그인
- JWT 기반 보안 인증
- 사용자 프로필 관리
- 개인정보 보호 설정

## 🛠️ 기술적 특징

### 마이크로서비스 아키텍처
- 백엔드 API와 AI 서비스 분리
- 서비스별 독립적인 확장성
- Docker 컨테이너 기반 배포

### 현대적 기술 스택
- **백엔드**: Spring Boot 3.5.3, Java 21, JPA/Hibernate
- **AI 서비스**: FastAPI, Python 3.11, OpenAI API
- **프론트엔드**: React Native 0.75.3, TypeScript
- **데이터베이스**: MySQL 8.0, Redis 7.2

### 성능 최적화
- 데이터베이스 인덱싱 및 쿼리 최적화
- Redis 캐싱 전략
- 연결 풀링 및 리소스 관리
- 압축 및 CDN 최적화

### 보안 강화
- JWT 토큰 기반 인증
- OAuth 2.0 소셜 로그인
- API 레이트 리미팅
- SQL Injection 방지
- XSS 보호

## 📈 확장성 고려사항

### 수평적 확장
- 로드 밸런서를 통한 트래픽 분산
- 마이크로서비스별 독립적 스케일링
- 데이터베이스 읽기 복제본 활용

### 성능 모니터링
- Prometheus + Grafana 메트릭 수집
- 애플리케이션 성능 모니터링 (APM)
- 로그 중앙화 (ELK Stack)
- 알림 및 경고 시스템

### 데이터 관리
- 정기 데이터베이스 백업
- 데이터 보관 정책
- GDPR 준수 개인정보 처리

## 🧪 품질 보증

### 테스트 커버리지
- **백엔드**: 85% 이상
- **AI 서비스**: 80% 이상
- **프론트엔드**: 75% 이상

### 테스트 전략
- TDD (Test-Driven Development) 적용
- 단위 테스트, 통합 테스트, E2E 테스트
- 자동화된 테스트 파이프라인
- 성능 테스트 및 부하 테스트

### 코드 품질
- 정적 코드 분석 (SonarQube)
- 코드 리뷰 프로세스
- 코딩 컨벤션 준수
- 보안 취약점 검사

## 🔄 개발 프로세스

### Git 워크플로우
- **main**: 운영 배포 브랜치
- **develop**: 개발 통합 브랜치
- **feature/*****: 기능 개발 브랜치
- **hotfix/*****: 긴급 수정 브랜치

### CI/CD 파이프라인
- GitHub Actions 기반 자동화
- 자동 테스트 실행
- 도커 이미지 빌드 및 배포
- 롤백 전략

### 코드 리뷰
- Pull Request 기반 코드 리뷰
- 최소 1명 이상의 승인 필수
- 자동화된 품질 검사
- 문서화 요구사항

## 📞 지원 및 기여

### 이슈 리포팅
- GitHub Issues를 통한 버그 리포트
- 기능 요청 및 개선 제안
- 문서 개선 요청

### 기여 방법
1. 저장소 포크
2. 기능 브랜치 생성
3. 변경사항 구현
4. 테스트 작성 및 실행
5. Pull Request 생성

### 연락처
- **프로젝트 관리자**: [admin@bookreview.com]
- **기술 지원**: [tech-support@bookreview.com]
- **GitHub Issues**: [프로젝트 저장소 Issues 페이지]

---

## 📜 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

## 🙏 감사의 말

이 프로젝트는 현대적인 웹 개발 기술과 AI 기술을 활용하여 독서 경험을 향상시키는 것을 목표로 합니다. 오픈소스 커뮤니티와 기여해주신 모든 분들께 감사드립니다.

---

**마지막 업데이트**: 2024년 1월  
**문서 버전**: 1.0.0