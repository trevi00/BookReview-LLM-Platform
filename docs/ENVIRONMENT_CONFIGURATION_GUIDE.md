# 환경별 설정 가이드

## 📋 개요

BookReview-LLM-Platform 백엔드의 개발, 테스트, 프로덕션 환경별 설정 방법과 최적화 가이드입니다. 각 환경에 적합한 구성과 보안 설정을 제공합니다.

---

## 🏗️ 환경 구성 개요

### 환경 분류
```
🔧 Development (dev)    - 로컬 개발 환경
🧪 Test (test)         - 자동화 테스트 환경  
🚀 Production (prod)   - 운영 환경
```

### 환경별 차이점

| 설정 항목 | 개발 환경 | 테스트 환경 | 프로덕션 환경 |
|-----------|-----------|-------------|---------------|
| **데이터베이스** | 로컬 MySQL | 테스트 컨테이너 | 운영 MySQL |
| **Redis** | 로컬 Redis | 테스트 컨테이너 | 운영 Redis |
| **로깅 레벨** | DEBUG | INFO | WARN |
| **Swagger UI** | ✅ 활성화 | ✅ 활성화 | ❌ 비활성화 |
| **Actuator** | 전체 노출 | 제한적 노출 | 최소 노출 |
| **에러 스택트레이스** | ✅ 포함 | ✅ 포함 | ❌ 제외 |
| **CORS** | localhost 허용 | 테스트 도메인 | 특정 도메인만 |

---

## 🔧 개발 환경 (Development)

### application-dev.yml
```yaml
# 개발 환경 설정
spring:
  # 개발용 데이터베이스 설정
  datasource:
    url: jdbc:mysql://localhost:3307/bookreview_dev?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: dev_user
    password: dev_password
    hikari:
      maximum-pool-size: 10  # 개발환경은 작은 풀 사이즈
      minimum-idle: 2
      connection-timeout: 20000
  
  # JPA 개발 설정
  jpa:
    hibernate:
      ddl-auto: update  # 개발환경에서는 스키마 자동 업데이트
    properties:
      hibernate:
        show_sql: true   # SQL 쿼리 출력
        format_sql: true # SQL 포맷팅
        use_sql_comments: true
    show-sql: true
  
  # 개발용 Redis 설정
  data:
    redis:
      host: localhost
      port: 6379
      password: # 개발환경은 비밀번호 없음
      database: 0  # 개발용 DB 인덱스

# 개발 환경 로깅
logging:
  level:
    com.bookreview: DEBUG    # 애플리케이션 로그는 상세히
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    root: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# 개발용 JWT 설정 (짧은 만료시간으로 테스트 용이하게)
jwt:
  secret: ${JWT_SECRET:dev-secret-key-for-local-development-only-change-in-production}
  expiration: 3600  # 1시간
  refresh-expiration: 86400  # 24시간

# 개발용 AI 서비스
ai:
  service:
    url: http://localhost:8000
    timeout: 30s

# 개발용 파일 업로드
file:
  upload:
    path: ./dev-uploads
    max-size: 50MB  # 개발환경은 큰 파일 허용

# 개발 환경 액추에이터 (모든 엔드포인트 노출)
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# 개발용 서버 설정
server:
  port: 8080
  error:
    include-stacktrace: always      # 개발환경은 스택트레이스 포함
    include-message: always
    include-binding-errors: always

# 개발용 OpenAPI 설정
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true
    filter: true
    tags-sorter: alpha
```

### 개발 환경 Docker Compose
```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  mysql-dev:
    image: mysql:8.0
    container_name: bookreview-mysql-dev
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: bookreview_dev
      MYSQL_USER: dev_user
      MYSQL_PASSWORD: dev_password
    ports:
      - "3307:3306"
    volumes:
      - mysql_dev_data:/var/lib/mysql
      - ./database/init:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password

  redis-dev:
    image: redis:6-alpine
    container_name: bookreview-redis-dev
    ports:
      - "6379:6379"
    volumes:
      - redis_dev_data:/data

  ai-service-dev:
    build:
      context: ./ai-service
      dockerfile: Dockerfile.dev
    container_name: bookreview-ai-dev
    ports:
      - "8000:8000"
    environment:
      - ENVIRONMENT=development
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./ai-service:/app
      - /app/node_modules

volumes:
  mysql_dev_data:
  redis_dev_data:
```

### 개발 환경 시작 스크립트
```bash
#!/bin/bash
# start-dev.sh

echo "🔧 Starting Development Environment..."

# Docker 컨테이너 시작
docker-compose -f docker-compose.dev.yml up -d

# 컨테이너 상태 확인
echo "⏳ Waiting for services to be ready..."
sleep 10

# 데이터베이스 연결 확인
echo "🔍 Checking database connection..."
until docker exec bookreview-mysql-dev mysqladmin ping -h"localhost" --silent; do
    echo "Waiting for MySQL..."
    sleep 2
done

echo "✅ Database is ready!"

# Redis 연결 확인
echo "🔍 Checking Redis connection..."
until docker exec bookreview-redis-dev redis-cli ping | grep PONG; do
    echo "Waiting for Redis..."
    sleep 2
done

echo "✅ Redis is ready!"

# Spring Boot 애플리케이션 시작
echo "🚀 Starting Spring Boot application..."
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun

echo "🎉 Development environment is ready!"
echo "📝 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "📊 Actuator: http://localhost:8080/actuator"
```

---

## 🧪 테스트 환경 (Test)

### application-test.yml
```yaml
# 테스트 환경 설정
spring:
  # 테스트용 인메모리 데이터베이스 (빠른 테스트)
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  
  # 테스트용 JPA 설정
  jpa:
    hibernate:
      ddl-auto: create-drop  # 테스트 시작 시 생성, 종료 시 삭제
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        show_sql: false  # 테스트 시 SQL 로그 최소화
        format_sql: false
    show-sql: false
  
  # 테스트용 Redis (Embedded Redis 사용)
  data:
    redis:
      host: localhost
      port: 0  # 랜덤 포트 사용
      timeout: 1000ms

# 테스트 로깅 (간소화)
logging:
  level:
    com.bookreview: INFO
    org.springframework: WARN
    org.hibernate: WARN
    root: WARN
  pattern:
    console: "%d{HH:mm:ss} %-5level %logger{36} - %msg%n"

# 테스트용 JWT 설정 (짧은 만료시간)
jwt:
  secret: test-secret-key-for-automated-testing-environment-only
  expiration: 60   # 1분 (빠른 테스트)
  refresh-expiration: 300  # 5분

# 테스트용 액추에이터
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

# 테스트 서버 설정
server:
  port: 0  # 랜덤 포트 사용 (포트 충돌 방지)

# 테스트용 OpenAPI (활성화)
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

### TestContainers 설정
```java
@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.yml")
public abstract class IntegrationTestBase {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // 컨테이너 재사용으로 성능 향상
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379)
            .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 데이터베이스 연결 정보 동적 설정
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Redis 연결 정보 동적 설정
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}
```

### 테스트 실행 스크립트
```bash
#!/bin/bash
# run-tests.sh

echo "🧪 Running Test Suite..."

# 단위 테스트 실행
echo "📝 Running Unit Tests..."
./gradlew test --tests "*Test" --continue

# 통합 테스트 실행
echo "🔗 Running Integration Tests..."
./gradlew test --tests "*IntegrationTest" --continue

# 보안 테스트 실행
echo "🔒 Running Security Tests..."
./gradlew test --tests "*SecurityTest" --continue

# 테스트 커버리지 리포트 생성
echo "📊 Generating Coverage Report..."
./gradlew jacocoTestReport

# SonarQube 분석 실행 (선택사항)
if [ "$SONAR_ENABLED" = "true" ]; then
    echo "🔍 Running SonarQube Analysis..."
    ./gradlew sonarqube
fi

echo "✅ All tests completed!"
echo "📊 Coverage Report: build/reports/jacoco/test/html/index.html"
```

---

## 🚀 프로덕션 환경 (Production)

### application-prod.yml
```yaml
# 운영 환경 설정
spring:
  # 운영용 데이터베이스 설정
  datasource:
    url: ${DB_URL:jdbc:mysql://mysql-prod:3306/bookreview?useSSL=true&serverTimezone=Asia/Seoul}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 50      # 운영환경은 큰 풀 사이즈
      minimum-idle: 10
      idle-timeout: 600000       # 10분
      max-lifetime: 1800000      # 30분
      connection-timeout: 30000   # 30초
      leak-detection-threshold: 60000  # 연결 누수 감지
  
  # 운영용 JPA 설정
  jpa:
    hibernate:
      ddl-auto: validate  # 운영환경에서는 스키마 검증만
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        show_sql: false   # 운영환경에서는 SQL 로그 비활성화
        format_sql: false
        jdbc:
          batch_size: 50  # 배치 사이즈 최적화
        order_inserts: true
        order_updates: true
        generate_statistics: false  # 통계 수집 비활성화
    show-sql: false
    open-in-view: false  # OSIV 비활성화로 성능 향상
  
  # 운영용 Redis 설정
  data:
    redis:
      host: ${REDIS_HOST:redis-prod}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      timeout: 3000ms
      database: 0
      lettuce:
        pool:
          max-active: 20
          max-wait: 2000ms
          max-idle: 10
          min-idle: 5

# 운영 환경 로깅 (최소화)
logging:
  level:
    com.bookreview: INFO
    org.springframework: WARN
    org.springframework.security: WARN
    org.hibernate: WARN
    root: WARN
  file:
    name: /app/logs/application.log
    max-size: 100MB
    max-history: 30
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n"
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# 운영용 JWT 설정 (환경변수 필수)
jwt:
  secret: ${JWT_SECRET}  # 필수 환경변수
  expiration: ${JWT_EXPIRATION:28800}  # 8시간
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:2592000}  # 30일

# 운영용 AI 서비스
ai:
  service:
    url: ${AI_SERVICE_URL:http://ai-service:8000}
    timeout: ${AI_SERVICE_TIMEOUT:30s}
    retry:
      max-attempts: 3
      backoff-delay: 2s

# 운영용 파일 업로드
file:
  upload:
    path: ${FILE_UPLOAD_PATH:/app/uploads}
    max-size: ${FILE_MAX_SIZE:10MB}

# 운영 환경 액추에이터 (최소 노출)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized  # 인증된 사용자만
      roles: ADMIN
  metrics:
    export:
      prometheus:
        enabled: true
        step: 30s
  security:
    enabled: true

# 운영 서버 설정
server:
  port: 8080
  servlet:
    context-path: /
  error:
    include-stacktrace: never       # 운영환경에서는 스택트레이스 숨김
    include-message: never
    include-binding-errors: never
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
    min-response-size: 1024
  # SSL 설정
  ssl:
    enabled: ${SSL_ENABLED:false}
    key-store: ${SSL_KEY_STORE}
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
    key-store-type: PKCS12

# 운영용 OpenAPI (비활성화)
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

### 프로덕션 Docker 설정
```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: bookreview-backend-prod
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=${DB_URL}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis-prod
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - AI_SERVICE_URL=http://ai-service:8000
    depends_on:
      - mysql-prod
      - redis-prod
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  mysql-prod:
    image: mysql:8.0
    container_name: bookreview-mysql-prod
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=bookreview
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql_prod_data:/var/lib/mysql
      - ./database/init:/docker-entrypoint-initdb.d
    networks:
      - app-network
    restart: unless-stopped
    command: >
      --default-authentication-plugin=mysql_native_password
      --innodb-buffer-pool-size=1G
      --innodb-log-file-size=256M
      --max-connections=200

  redis-prod:
    image: redis:6-alpine
    container_name: bookreview-redis-prod
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_prod_data:/data
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
    networks:
      - app-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: bookreview-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - backend
    networks:
      - app-network
    restart: unless-stopped

volumes:
  mysql_prod_data:
  redis_prod_data:

networks:
  app-network:
    driver: bridge
```

### 프로덕션 Nginx 설정
```nginx
# nginx/nginx.conf
upstream backend {
    server backend:8080;
}

server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    
    # HTTP to HTTPS 리다이렉트
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    # SSL 설정
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;
    
    # 보안 헤더
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # 압축 설정
    gzip on;
    gzip_types text/plain application/json application/javascript text/css;
    
    # API 프록시
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 타임아웃 설정
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
    
    # 정적 파일 서빙
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # 로그 설정
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;
}
```

---

## 🔒 환경별 보안 설정

### 환경 변수 관리

#### 개발 환경 (.env.dev)
```bash
# 개발 환경 변수
SPRING_PROFILES_ACTIVE=dev

# 데이터베이스
DB_HOST=localhost
DB_PORT=3307
DB_NAME=bookreview_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT (개발용 키)
JWT_SECRET=dev-secret-key-for-local-development-only-change-in-production
JWT_EXPIRATION=3600

# AI 서비스
OPENAI_API_KEY=your-dev-openai-key
AI_SERVICE_URL=http://localhost:8000

# 로깅
LOG_LEVEL=DEBUG
```

#### 프로덕션 환경 (환경변수 또는 보안 볼트)
```bash
# 프로덕션 환경 변수 (예시 - 실제로는 보안 볼트 사용)
SPRING_PROFILES_ACTIVE=prod

# 데이터베이스 (강력한 패스워드)
DB_URL=jdbc:mysql://prod-mysql.amazonaws.com:3306/bookreview
DB_USERNAME=prod_user
DB_PASSWORD=StR0ng_Pr0d_P@ssw0rd_2024!

# Redis (보안 설정)
REDIS_HOST=prod-redis.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=R3d1s_Pr0d_P@ssw0rd_2024!

# JWT (256비트 이상 강력한 키)
JWT_SECRET=Pr0duct10n_JWT_S3cr3t_K3y_256_B1ts_0r_M0r3_F0r_M@x1mum_S3cur1ty_2024!
JWT_EXPIRATION=28800

# SSL
SSL_ENABLED=true
SSL_KEY_STORE=/app/ssl/keystore.p12
SSL_KEY_STORE_PASSWORD=SSL_K3yst0r3_P@ssw0rd!

# AI 서비스
OPENAI_API_KEY=your-production-openai-key
AI_SERVICE_URL=https://ai-service.yourdomain.com

# 로깅
LOG_LEVEL=INFO
```

### AWS Secrets Manager 사용 예시
```java
@Configuration
@Profile("prod")
public class AwsSecretsConfig {
    
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .build();
    }
    
    @Bean
    @Primary
    public DataSource dataSource(SecretsManagerClient secretsClient) {
        // AWS Secrets Manager에서 DB 자격증명 가져오기
        String secretValue = getSecret(secretsClient, "prod/bookreview/db");
        DatabaseCredentials credentials = parseCredentials(secretValue);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(credentials.getUrl());
        config.setUsername(credentials.getUsername());
        config.setPassword(credentials.getPassword());
        
        return new HikariDataSource(config);
    }
}
```

---

## 🔄 환경 전환 가이드

### 개발 → 테스트 환경 전환
```bash
#!/bin/bash
# deploy-to-test.sh

echo "🔄 Deploying to Test Environment..."

# 1. 테스트 환경 변수 설정
export SPRING_PROFILES_ACTIVE=test
export DATABASE_URL=${TEST_DATABASE_URL}

# 2. 애플리케이션 빌드
./gradlew clean build -Dspring.profiles.active=test

# 3. 테스트 실행
./gradlew test integrationTest

# 4. Docker 이미지 빌드
docker build -t bookreview-backend:test .

# 5. 테스트 환경 배포
docker-compose -f docker-compose.test.yml up -d

echo "✅ Test deployment completed!"
```

### 테스트 → 프로덕션 환경 전환
```bash
#!/bin/bash
# deploy-to-production.sh

echo "🚀 Deploying to Production Environment..."

# 1. 사전 체크
echo "🔍 Pre-deployment checks..."

# 환경변수 검증
required_vars=("JWT_SECRET" "DB_PASSWORD" "REDIS_PASSWORD")
for var in "${required_vars[@]}"; do
    if [[ -z "${!var}" ]]; then
        echo "❌ Required environment variable $var is not set"
        exit 1
    fi
done

# 2. 백업 생성
echo "💾 Creating database backup..."
docker exec mysql-prod mysqldump -u root -p${MYSQL_ROOT_PASSWORD} bookreview > backup_$(date +%Y%m%d_%H%M%S).sql

# 3. 블루-그린 배포
echo "🔄 Starting blue-green deployment..."

# 새 버전 컨테이너 시작 (green)
docker-compose -f docker-compose.prod.yml up -d --scale backend=2

# 헬스체크 대기
sleep 30

# 로드밸런서에서 이전 버전 제거 (blue)
echo "⚖️ Switching traffic to new version..."
# nginx 설정 업데이트 또는 로드밸런서 설정

# 4. 배포 검증
echo "✅ Verifying deployment..."
curl -f http://localhost:8080/actuator/health || exit 1

echo "🎉 Production deployment completed successfully!"
```

---

## 📊 모니터링 및 알림

### Prometheus 설정
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'bookreview-backend'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
```

### Grafana 대시보드 설정
```json
{
  "dashboard": {
    "title": "BookReview Backend Monitoring",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ]
      },
      {
        "title": "Response Time",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"4..|5..\"}[5m])"
          }
        ]
      },
      {
        "title": "Database Connections",
        "targets": [
          {
            "expr": "hikaricp_connections_active"
          }
        ]
      }
    ]
  }
}
```

### 알림 규칙
```yaml
# alerting-rules.yml
groups:
  - name: bookreview-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          
      - alert: DatabaseConnectionsHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool usage is high"
          
      - alert: JWTTokenBlacklistSizeHigh
        expr: redis_keys{db="0",key_pattern="blacklist:*"} > 10000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "JWT blacklist size is growing large"
```

---

## 🔧 성능 최적화

### JVM 설정

#### 개발 환경 JVM 옵션
```bash
# 개발환경 (메모리 절약)
JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

#### 프로덕션 환경 JVM 옵션
```bash
# 프로덕션 환경 (성능 최적화)
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+UseCompressedOops \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/dumps/ \
  -Djava.security.egd=file:/dev/./urandom"
```

### 데이터베이스 최적화

#### MySQL 설정 (my.cnf)
```ini
# 프로덕션 MySQL 설정
[mysqld]
# 기본 설정
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 성능 최적화
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_file_per_table = 1

# 연결 설정
max_connections = 200
wait_timeout = 600
interactive_timeout = 600

# 로깅
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
```

### Redis 최적화

#### Redis 설정 (redis.conf)
```conf
# 메모리 설정
maxmemory 1gb
maxmemory-policy allkeys-lru

# 영속성 설정 (토큰 블랙리스트는 휘발성 데이터)
save ""
appendonly no

# 네트워크 최적화
tcp-keepalive 300
timeout 300

# 로깅
loglevel notice
logfile /var/log/redis/redis-server.log
```

---

## 🚨 문제 해결 가이드

### 자주 발생하는 문제들

#### 1. 환경별 설정 로드 실패
```bash
# 문제: 프로파일이 제대로 로드되지 않음
# 해결책: 환경변수 확인
echo $SPRING_PROFILES_ACTIVE

# 또는 application.yml에서 직접 설정
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

#### 2. 데이터베이스 연결 실패
```java
// 연결 풀 상태 모니터링
@Component
public class DatabaseHealthChecker {
    
    @Autowired
    private DataSource dataSource;
    
    @EventListener
    @Async
    public void checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            if (!isValid) {
                log.error("Database connection is not valid!");
                // 알림 발송 로직
            }
        } catch (SQLException e) {
            log.error("Database connection failed", e);
        }
    }
}
```

#### 3. Redis 연결 문제
```java
@Component
public class RedisHealthChecker {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean isRedisAvailable() {
        try {
            String result = redisTemplate.execute(connection -> {
                return connection.ping();
            });
            return "PONG".equals(result);
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }
}
```

#### 4. 메모리 부족 문제
```bash
# 힙 덤프 분석
jmap -dump:format=b,file=heapdump.hprof <pid>

# 메모리 사용량 모니터링
jstat -gc <pid> 5s
```

---

## 📚 체크리스트

### 환경 설정 체크리스트

#### 🔧 개발 환경
- [ ] 로컬 MySQL/Redis 설치 및 실행
- [ ] IDE 환경변수 설정
- [ ] Hot Reload 설정
- [ ] 디버깅 포트 설정
- [ ] 테스트 데이터 준비

#### 🧪 테스트 환경  
- [ ] TestContainers 설정
- [ ] CI/CD 파이프라인 구성
- [ ] 테스트 데이터베이스 격리
- [ ] 병렬 테스트 실행 설정
- [ ] 커버리지 리포트 생성

#### 🚀 프로덕션 환경
- [ ] 환경변수 보안 설정
- [ ] SSL/TLS 인증서 설정
- [ ] 로드밸런서 구성
- [ ] 백업 전략 수립
- [ ] 모니터링 대시보드 구축
- [ ] 알림 시스템 구성
- [ ] 장애 복구 계획 수립

---

## 📞 문의 및 지원

**DevOps 팀**: devops@bookreview.com  
**인프라 문의**: infrastructure@bookreview.com  
**환경 설정 지원**: env-support@bookreview.com  

환경 설정 관련 문의나 배포 이슈가 있으시면 언제든지 연락해 주시기 바랍니다.

**문서 버전**: v1.0  
**최종 업데이트**: 2025-07-23