# BookReview LLM Platform - Mobile App

AI 피드백과 함께하는 스마트 독서 플랫폼의 React Native 모바일 앱입니다.

## 기능

- 📚 책 등록 및 관리
- 📝 챕터별 독서 노트 작성
- 🤖 AI 기반 개인화된 피드백
- 📊 독서 목표 설정 및 통계
- 🔍 책 검색 및 추천
- 👥 사용자 인증 (로컬 및 Google OAuth)

## 기술 스택

- **Framework**: React Native 0.75.3
- **Language**: TypeScript
- **State Management**: Context API + Hooks
- **Navigation**: React Navigation (예정)
- **HTTP Client**: Fetch API
- **Authentication**: JWT + OAuth2

## 설치 및 실행

### 사전 요구사항

- Node.js 18+
- React Native CLI
- Android Studio (Android 개발시)
- Xcode (iOS 개발시)

### 설치

```bash
# 의존성 설치
npm install

# iOS 포드 설치 (iOS만)
cd ios && pod install && cd ..
```

### 개발 서버 실행

```bash
# Metro 서버 시작
npm start

# Android 실행
npm run android

# iOS 실행
npm run ios
```

## 프로젝트 구조

```
src/
├── components/        # 재사용 가능한 컴포넌트
├── screens/          # 화면 컴포넌트
├── navigation/       # 네비게이션 설정
├── services/         # API 서비스
├── hooks/           # 커스텀 훅
├── types/           # TypeScript 타입 정의
├── utils/           # 유틸리티 함수
└── assets/          # 정적 자원 (이미지, 폰트 등)
```

## API 연동

백엔드 서버와의 통신을 위한 API 클라이언트가 구성되어 있습니다:

- **Backend API**: `http://localhost:8080/api/v1`
- **AI Service**: `http://localhost:8000`

### 주요 서비스

- `authService`: 사용자 인증 관련
- `bookService`: 책 및 독서 기록 관련

## 개발 가이드

### 코드 스타일

```bash
# 린트 실행
npm run lint

# 타입 체크
npm run typecheck
```

### 테스트

```bash
# 테스트 실행
npm test
```

## 빌드 및 배포

### Android

```bash
# APK 빌드
cd android && ./gradlew assembleRelease

# AAB 빌드
cd android && ./gradlew bundleRelease
```

### iOS

```bash
# Archive 빌드 (Xcode에서)
npx react-native run-ios --configuration Release
```

## 환경 설정

개발 환경에 따른 설정은 다음과 같이 관리됩니다:

- `.env.development`: 개발 환경 설정
- `.env.production`: 운영 환경 설정

## 주요 의존성

- `react`: UI 라이브러리
- `react-native`: 크로스 플랫폼 프레임워크
- `typescript`: 정적 타입 체킹
- `@react-native/eslint-config`: 코드 스타일 검사

## 라이센스

MIT License