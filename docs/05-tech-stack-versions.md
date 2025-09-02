# 기술 스택 및 버전 정의

## 1. 백엔드 (Spring Boot)

### 1.1 핵심 프레임워크
```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.3'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'jacoco'
    id 'org.sonarqube' version '4.4.1.3373'
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    
    // Database
    runtimeOnly 'com.mysql:mysql-connector-j:8.2.0'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
    
    // Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // Monitoring
    runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
    
    // Test Dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
    testImplementation 'org.testcontainers:mysql:1.19.3'
    testImplementation 'com.h2database:h2'
}
```

### 1.2 주요 라이브러리 버전
- **Java**: 21 (OpenJDK Temurin)
- **Spring Boot**: 3.5.3
- **Spring Security**: 6.x (Spring Boot에 포함)
- **Spring Data JPA**: 3.x (Spring Boot에 포함)
- **MySQL Connector**: 8.2.0
- **Redis**: 7.2 (Lettuce 클라이언트)
- **JWT (JJWT)**: 0.12.3
- **SpringDoc OpenAPI**: 2.3.0
- **TestContainers**: 1.19.3

### 1.3 빌드 도구 설정
```gradle
// gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip

// gradle.properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
```

## 2. AI 서비스 (FastAPI)

### 2.1 Python 환경 설정
```yaml
# environment.yml (Anaconda)
name: bookreview-ai
channels:
  - conda-forge
  - defaults
dependencies:
  - python=3.11.7
  - pip=23.3.1
  - numpy=1.24.3
  - pandas=2.0.3
  - pip:
    - fastapi==0.104.1
    - uvicorn[standard]==0.24.0
    - pydantic==2.5.0
    - httpx==0.25.2
    - python-multipart==0.0.6
    - python-jose[cryptography]==3.3.0
    - passlib[bcrypt]==1.7.4
    - redis==5.0.1
    - sqlalchemy==2.0.23
    - alembic==1.13.1
    - openai==1.3.7
    - tiktoken==0.5.2
    - langchain==0.0.350
    - langchain-openai==0.0.2
    - pytest==7.4.3
    - pytest-asyncio==0.21.1
    - pytest-cov==4.1.0
    - black==23.11.0
    - flake8==6.1.0
    - mypy==1.7.1
```

### 2.2 핵심 라이브러리
- **Python**: 3.11.7
- **FastAPI**: 0.104.1
- **Uvicorn**: 0.24.0 (ASGI 서버)
- **Pydantic**: 2.5.0 (데이터 검증)
- **OpenAI**: 1.3.7 (AI API 클라이언트)
- **LangChain**: 0.0.350 (LLM 프레임워크)
- **Redis**: 5.0.1 (캐시 클라이언트)
- **SQLAlchemy**: 2.0.23 (ORM, 필요시)
- **pytest**: 7.4.3 (테스트 프레임워크)

### 2.3 requirements.txt
```txt
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0
pydantic-settings==2.1.0
httpx==0.25.2
python-multipart==0.0.6
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
redis==5.0.1
openai==1.3.7
tiktoken==0.5.2
langchain==0.0.350
langchain-openai==0.0.2
prometheus-client==0.19.0
structlog==23.2.0
tenacity==8.2.3

# Development dependencies
pytest==7.4.3
pytest-asyncio==0.21.1
pytest-cov==4.1.0
pytest-mock==3.12.0
black==23.11.0
flake8==6.1.0
mypy==1.7.1
isort==5.12.0
pre-commit==3.6.0
```

## 3. 프론트엔드 (React Native)

### 3.1 React Native 환경
```json
{
  "name": "bookreview-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "android": "react-native run-android",
    "ios": "react-native run-ios",
    "web": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "lint": "eslint . --ext .js,.jsx,.ts,.tsx",
    "type-check": "tsc --noEmit"
  },
  "dependencies": {
    "react": "18.2.0",
    "react-native": "0.73.2",
    "react-native-web": "0.19.9",
    "react-scripts": "5.0.1",
    "typescript": "5.3.3",
    "@types/react": "18.2.45",
    "@types/react-native": "0.72.8",
    
    "react-navigation": "^6.0.0",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/stack": "^6.3.20",
    "@react-navigation/bottom-tabs": "^6.5.11",
    
    "react-query": "^3.39.3",
    "@tanstack/react-query": "^5.12.2",
    "axios": "^1.6.2",
    
    "react-hook-form": "^7.48.2",
    "yup": "^1.4.0",
    "@hookform/resolvers": "^3.3.2",
    
    "react-native-google-signin": "^10.1.1",
    "react-native-keychain": "^8.1.3",
    "react-native-encrypted-storage": "^4.0.3",
    
    "react-native-paper": "^5.11.6",
    "react-native-vector-icons": "^10.0.3",
    "react-native-safe-area-context": "^4.8.2",
    "react-native-screens": "^3.29.0"
  },
  "devDependencies": {
    "@babel/core": "^7.23.6",
    "@babel/preset-env": "^7.23.6",
    "@babel/runtime": "^7.23.6",
    "@react-native/babel-preset": "^0.73.18",
    "@react-native/eslint-config": "^0.73.1",
    "@react-native/metro-config": "^0.73.2",
    "@react-native/typescript-config": "^0.73.1",
    "@types/jest": "^29.5.8",
    "babel-jest": "^29.7.0",
    "eslint": "^8.55.0",
    "jest": "^29.7.0",
    "metro-react-native-babel-preset": "^0.77.0",
    "prettier": "^3.1.1",
    "react-test-renderer": "18.2.0"
  }
}
```

### 3.2 주요 라이브러리 버전
- **React**: 18.2.0
- **React Native**: 0.73.2
- **TypeScript**: 5.3.3
- **React Navigation**: 6.1.9
- **TanStack Query**: 5.12.2 (상태 관리)
- **Axios**: 1.6.2 (HTTP 클라이언트)
- **React Hook Form**: 7.48.2 (폼 관리)
- **React Native Paper**: 5.11.6 (UI 컴포넌트)
- **Yup**: 1.4.0 (스키마 검증)

### 3.3 개발 도구 설정
```json
// tsconfig.json
{
  "extends": "@react-native/typescript-config/tsconfig.json",
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": "./src",
    "paths": {
      "@/*": ["*"],
      "@components/*": ["components/*"],
      "@screens/*": ["screens/*"],
      "@services/*": ["services/*"],
      "@utils/*": ["utils/*"],
      "@types/*": ["types/*"]
    }
  }
}
```

```javascript
// .eslintrc.js
module.exports = {
  root: true,
  extends: [
    '@react-native',
    '@typescript-eslint/recommended',
    'prettier'
  ],
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  rules: {
    'react-hooks/exhaustive-deps': 'error',
    '@typescript-eslint/no-unused-vars': 'error',
    '@typescript-eslint/explicit-function-return-type': 'warn',
    'react-native/no-inline-styles': 'error',
    'react-native/no-color-literals': 'warn'
  }
};
```

## 4. 데이터베이스

### 4.1 MySQL 설정
- **MySQL**: 8.0.35
- **MySQL Connector/J**: 8.2.0
- **Character Set**: utf8mb4
- **Collation**: utf8mb4_unicode_ci
- **Engine**: InnoDB
- **Connection Pool**: HikariCP (Spring Boot 기본)

### 4.2 Redis 설정
- **Redis**: 7.2-alpine
- **Java Client**: Lettuce (Spring Boot 기본)
- **Python Client**: redis-py 5.0.1
- **Connection Pool**: 활성화
- **Max Memory**: 512MB (개발환경)

## 5. 컨테이너 및 인프라

### 5.1 Docker 버전
```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# ai-service/Dockerfile
FROM python:3.11.7-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 5.2 개발 도구
- **Docker**: 24.0.7
- **Docker Compose**: 2.21.0
- **Node.js**: 18.18.2 (LTS)
- **npm**: 9.8.1
- **Gradle**: 8.5
- **Maven**: 3.9.5 (선택사항)

## 6. 테스트 도구

### 6.1 백엔드 테스트
```gradle
dependencies {
    // JUnit 5
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
    
    // Mockito
    testImplementation 'org.mockito:mockito-core:5.7.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.7.0'
    
    // AssertJ
    testImplementation 'org.assertj:assertj-core:3.24.2'
    
    // TestContainers
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
    testImplementation 'org.testcontainers:mysql:1.19.3'
    testImplementation 'org.testcontainers:redis:1.19.3'
    
    // WireMock
    testImplementation 'com.github.tomakehurst:wiremock-jre8:3.0.1'
    
    // H2 Database (테스트용)
    testRuntimeOnly 'com.h2database:h2:2.2.224'
}
```

### 6.2 AI 서비스 테스트
```txt
# AI 서비스 테스트 의존성
pytest==7.4.3
pytest-asyncio==0.21.1
pytest-cov==4.1.0
pytest-mock==3.12.0
httpx==0.25.2  # 테스트 클라이언트
faker==20.1.0  # 테스트 데이터 생성
factory-boy==3.3.0  # 팩토리 패턴
```

### 6.3 프론트엔드 테스트
```json
{
  "devDependencies": {
    "@testing-library/react-native": "^12.4.2",
    "@testing-library/jest-native": "^5.4.3",
    "jest": "^29.7.0",
    "react-test-renderer": "18.2.0",
    "detox": "^20.13.5"
  }
}
```

## 7. 코드 품질 도구

### 7.1 정적 분석 도구
```gradle
// Checkstyle
id 'checkstyle'
checkstyle {
    toolVersion = '10.12.5'
    configFile = file("config/checkstyle/checkstyle.xml")
}

// SpotBugs
id 'com.github.spotbugs' version '5.2.1'
spotbugs {
    toolVersion = '4.8.2'
}

// PMD
id 'pmd'
pmd {
    toolVersion = '6.55.0'
    ruleSetFiles = files("config/pmd/pmd-rules.xml")
}
```

### 7.2 코드 포맷터
```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json

  - repo: https://github.com/psf/black
    rev: 23.11.0
    hooks:
      - id: black
        language_version: python3.11

  - repo: https://github.com/pycqa/isort
    rev: 5.12.0
    hooks:
      - id: isort

  - repo: https://github.com/pycqa/flake8
    rev: 6.1.0
    hooks:
      - id: flake8

  - repo: https://github.com/pre-commit/mirrors-prettier
    rev: v3.1.0
    hooks:
      - id: prettier
        types_or: [javascript, jsx, ts, tsx, json, markdown]
```

## 8. 보안 라이브러리

### 8.1 백엔드 보안
```gradle
dependencies {
    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
    
    // Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // HTTPS/TLS
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### 8.2 프론트엔드 보안
```json
{
  "dependencies": {
    "react-native-keychain": "^8.1.3",
    "react-native-encrypted-storage": "^4.0.3",
    "react-native-google-signin": "^10.1.1",
    "crypto-js": "^4.2.0"
  }
}
```

## 9. 모니터링 및 로깅

### 9.1 메트릭 수집
```gradle
dependencies {
    // Micrometer (Prometheus)
    implementation 'io.micrometer:micrometer-registry-prometheus'
    
    // Actuator
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### 9.2 로깅 프레임워크
- **Java**: Logback (Spring Boot 기본)
- **Python**: structlog 23.2.0
- **JavaScript**: react-native-logs

## 10. 호환성 매트릭스

### 10.1 운영체제 지원
| 구성요소 | Windows | macOS | Linux |
|---------|---------|-------|-------|
| 백엔드 개발 | ✅ | ✅ | ✅ |
| AI 서비스 | ✅ | ✅ | ✅ |
| 프론트엔드 | ✅ | ✅ | ✅ |
| Docker | ✅ | ✅ | ✅ |

### 10.2 브라우저 지원
| 브라우저 | 버전 | 지원 상태 |
|---------|------|-----------|
| Chrome | 120+ | ✅ 완전 지원 |
| Firefox | 115+ | ✅ 완전 지원 |
| Safari | 16+ | ✅ 완전 지원 |
| Edge | 120+ | ✅ 완전 지원 |

### 10.3 모바일 플랫폼
| 플랫폼 | 최소 버전 | 권장 버전 |
|--------|-----------|-----------|
| Android | 8.0 (API 26) | 13+ (API 33) |
| iOS | 12.0 | 16+ |

## 11. 업그레이드 계획

### 11.1 정기 업데이트 계획
- **분기별**: 보안 패치 및 마이너 업데이트
- **반기별**: 메이저 라이브러리 업데이트 검토
- **연간**: 플랫폼 버전 업그레이드 (Java, Python, Node.js)

### 11.2 의존성 관리
```gradle
// Gradle Versions Plugin
id 'com.github.ben-manes.versions' version '0.50.0'

task dependencyUpdates(type: DependencyUpdatesTask) {
    resolutionStrategy {
        componentSelection { rules ->
            rules.all { ComponentSelection selection ->
                boolean rejected = ['alpha', 'beta', 'rc', 'cr', 'm', 'preview', 'b', 'ea'].any { qualifier ->
                    selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-+]*/
                }
                if (rejected) {
                    selection.reject('Release candidate')
                }
            }
        }
    }
}
```

### 11.3 버전 호환성 테스트
- 새 버전 도입 전 전체 테스트 스위트 실행
- 성능 회귀 테스트
- 보안 취약점 스캔
- 사용자 수용 테스트