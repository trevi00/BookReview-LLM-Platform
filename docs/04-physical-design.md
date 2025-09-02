# 물리적 설계 - 데이터베이스 스키마 및 환경 설정

## 1. 데이터베이스 물리적 설계

### 1.1 MySQL 설정 및 최적화

#### 1.1.1 MySQL 컨테이너 설정
```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0.35
    container_name: bookreview-mysql
    environment:
      MYSQL_DATABASE: bookreview
      MYSQL_USER: bookreview_user
      MYSQL_PASSWORD: bookreview_password
      MYSQL_ROOT_PASSWORD: root_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./database/init:/docker-entrypoint-initdb.d
      - ./database/conf:/etc/mysql/conf.d
    command: --default-authentication-plugin=mysql_native_password
    networks:
      - bookreview-network

volumes:
  mysql_data:
    driver: local

networks:
  bookreview-network:
    driver: bridge
```

#### 1.1.2 MySQL 설정 파일 (my.cnf)
```ini
[mysqld]
# 기본 설정
default_authentication_plugin = mysql_native_password
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
init_connect = 'SET NAMES utf8mb4'

# 성능 최적화
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_file_per_table = 1

# 연결 설정
max_connections = 200
wait_timeout = 28800
interactive_timeout = 28800

# 로그 설정
general_log = 0
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2

[mysql]
default-character-set = utf8mb4

[client]
default-character-set = utf8mb4
```

### 1.2 테이블 생성 스크립트

#### 1.2.1 사용자 관련 테이블
```sql
-- users 테이블
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NULL COMMENT 'OAuth 사용자는 NULL 가능',
    username VARCHAR(50) NOT NULL,
    profile_image VARCHAR(500) NULL,
    provider ENUM('LOCAL', 'GOOGLE') NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_users_email (email),
    INDEX idx_users_provider (provider, provider_id),
    INDEX idx_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 1.2.2 책 관련 테이블
```sql
-- books 테이블
CREATE TABLE books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(200) NOT NULL,
    publisher VARCHAR(200) NULL,
    isbn VARCHAR(20) NULL UNIQUE,
    published_year INT NULL,
    description TEXT NULL,
    cover_image_url VARCHAR(500) NULL,
    total_pages INT NULL,
    category ENUM('FICTION', 'NON_FICTION', 'SCIENCE', 'TECHNOLOGY', 'HISTORY', 
                  'BIOGRAPHY', 'SELF_HELP', 'BUSINESS', 'EDUCATION', 'OTHER') 
             NOT NULL DEFAULT 'OTHER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_isbn (isbn),
    INDEX idx_books_category (category),
    FULLTEXT idx_books_search (title, author, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- user_books 테이블 (사용자-책 연결)
CREATE TABLE user_books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    status ENUM('NOT_STARTED', 'READING', 'COMPLETED', 'PAUSED') 
           NOT NULL DEFAULT 'NOT_STARTED',
    start_date DATE NULL,
    end_date DATE NULL,
    current_page INT NOT NULL DEFAULT 0,
    personal_rating INT NULL CHECK (personal_rating >= 1 AND personal_rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_user_books_user_id (user_id),
    INDEX idx_user_books_book_id (book_id),
    INDEX idx_user_books_status (status),
    INDEX idx_user_books_dates (start_date, end_date),
    UNIQUE KEY uk_user_book_once (user_id, book_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 1.2.3 목차 및 독서 기록 테이블
```sql
-- chapters 테이블
CREATE TABLE chapters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_book_id BIGINT NOT NULL,
    chapter_number INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    start_page INT NULL,
    end_page INT NULL,
    description TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_book_id) REFERENCES user_books(id) ON DELETE CASCADE,
    INDEX idx_chapters_user_book_id (user_book_id),
    INDEX idx_chapters_number (chapter_number),
    UNIQUE KEY uk_chapter_number_per_book (user_book_id, chapter_number),
    CHECK (start_page IS NULL OR end_page IS NULL OR start_page <= end_page)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- reading_notes 테이블
CREATE TABLE reading_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    note_type ENUM('IMPRESSION', 'LEARNING', 'QUESTION', 'QUOTE') 
              NOT NULL DEFAULT 'IMPRESSION',
    page_number INT NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reading_notes_chapter_id (chapter_id),
    INDEX idx_reading_notes_user_id (user_id),
    INDEX idx_reading_notes_type (note_type),
    INDEX idx_reading_notes_created_at (created_at),
    INDEX idx_reading_notes_page (page_number),
    FULLTEXT idx_reading_notes_content (content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 1.2.4 피드백 및 세션 테이블
```sql
-- feedbacks 테이블
CREATE TABLE feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reading_note_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    feedback_type ENUM('COMMENT', 'QUESTION', 'SUGGESTION') 
                  NOT NULL DEFAULT 'COMMENT',
    ai_model VARCHAR(50) NOT NULL,
    is_useful BOOLEAN NULL COMMENT '사용자 평가',
    user_rating INT NULL CHECK (user_rating >= 1 AND user_rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (reading_note_id) REFERENCES reading_notes(id) ON DELETE CASCADE,
    INDEX idx_feedbacks_note_id (reading_note_id),
    INDEX idx_feedbacks_type (feedback_type),
    INDEX idx_feedbacks_ai_model (ai_model),
    INDEX idx_feedbacks_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- reading_sessions 테이블
CREATE TABLE reading_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_book_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    pages_read INT NOT NULL DEFAULT 0,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_book_id) REFERENCES user_books(id) ON DELETE CASCADE,
    INDEX idx_reading_sessions_user_book_id (user_book_id),
    INDEX idx_reading_sessions_start_time (start_time),
    INDEX idx_reading_sessions_date (DATE(start_time)),
    CHECK (end_time IS NULL OR start_time <= end_time),
    CHECK (pages_read >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 1.2.5 목표 및 통계 테이블
```sql
-- reading_goals 테이블
CREATE TABLE reading_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year INT NOT NULL,
    target_books INT NOT NULL DEFAULT 0,
    target_pages INT NOT NULL DEFAULT 0,
    current_books INT NOT NULL DEFAULT 0,
    current_pages INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reading_goals_user_id (user_id),
    INDEX idx_reading_goals_year (year),
    UNIQUE KEY uk_user_year_goal (user_id, year),
    CHECK (target_books >= 0 AND target_pages >= 0),
    CHECK (current_books >= 0 AND current_pages >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 1.3 인덱스 최적화 전략

#### 1.3.1 복합 인덱스
```sql
-- 사용자별 책 목록 조회 최적화
CREATE INDEX idx_user_books_user_status ON user_books(user_id, status);

-- 사용자별 독서 기록 조회 최적화
CREATE INDEX idx_notes_user_type_date ON reading_notes(user_id, note_type, created_at);

-- 책별 최근 기록 조회 최적화
CREATE INDEX idx_notes_chapter_date ON reading_notes(chapter_id, created_at DESC);

-- 피드백 통계 조회 최적화
CREATE INDEX idx_feedbacks_model_date ON feedbacks(ai_model, created_at);
```

#### 1.3.2 파티셔닝 (향후 확장)
```sql
-- 날짜 기반 파티셔닝 (reading_notes 테이블)
ALTER TABLE reading_notes PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 1.4 뷰 생성

#### 1.4.1 독서 통계 뷰
```sql
-- 사용자별 독서 통계 뷰
CREATE VIEW user_reading_statistics AS
SELECT 
    u.id AS user_id,
    u.username,
    COUNT(DISTINCT ub.book_id) AS total_books,
    COUNT(DISTINCT CASE WHEN ub.status = 'COMPLETED' THEN ub.book_id END) AS completed_books,
    COUNT(DISTINCT CASE WHEN ub.status = 'READING' THEN ub.book_id END) AS currently_reading,
    COUNT(DISTINCT rn.id) AS total_notes,
    COUNT(DISTINCT f.id) AS total_feedbacks,
    AVG(CASE WHEN ub.status = 'COMPLETED' THEN ub.personal_rating END) AS avg_rating
FROM users u
LEFT JOIN user_books ub ON u.id = ub.user_id
LEFT JOIN chapters c ON ub.id = c.user_book_id
LEFT JOIN reading_notes rn ON c.id = rn.chapter_id
LEFT JOIN feedbacks f ON rn.id = f.reading_note_id
WHERE u.is_active = TRUE
GROUP BY u.id, u.username;

-- 월별 독서 활동 뷰
CREATE VIEW monthly_reading_activity AS
SELECT 
    user_id,
    YEAR(created_at) AS year,
    MONTH(created_at) AS month,
    COUNT(*) AS notes_count,
    COUNT(DISTINCT DATE(created_at)) AS active_days
FROM reading_notes 
GROUP BY user_id, YEAR(created_at), MONTH(created_at);
```

### 1.5 스토리지 및 백업 설정

#### 1.5.1 데이터 볼륨 관리
```bash
# MySQL 데이터 볼륨 생성
docker volume create --name mysql_data

# 백업 볼륨 생성
docker volume create --name mysql_backup

# 데이터 백업 스크립트
#!/bin/bash
BACKUP_DIR="/var/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="bookreview_backup_${DATE}.sql"

docker exec bookreview-mysql mysqldump \
  -u root -p$MYSQL_ROOT_PASSWORD \
  --single-transaction \
  --routines \
  --triggers \
  bookreview > "${BACKUP_DIR}/${BACKUP_FILE}"

# 30일 이상된 백업 파일 삭제
find "${BACKUP_DIR}" -name "*.sql" -mtime +30 -delete
```

#### 1.5.2 복제 설정 (향후 확장)
```sql
-- 마스터 서버 설정
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog-do-db = bookreview

-- 슬레이브 서버 설정
[mysqld]
server-id = 2
relay-log = mysql-relay-bin
```

## 2. 개발 환경 설정

### 2.1 Docker 개발 환경

#### 2.1.1 전체 개발 환경 docker-compose.yml
```yaml
version: '3.8'

services:
  # MySQL 데이터베이스
  mysql:
    image: mysql:8.0.35
    container_name: bookreview-mysql
    environment:
      MYSQL_DATABASE: bookreview
      MYSQL_USER: bookreview_user
      MYSQL_PASSWORD: bookreview_password
      MYSQL_ROOT_PASSWORD: root_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./database/init:/docker-entrypoint-initdb.d
      - ./database/conf:/etc/mysql/conf.d
    networks:
      - bookreview-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

  # Redis 캐시
  redis:
    image: redis:7.2-alpine
    container_name: bookreview-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      - ./redis/redis.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    networks:
      - bookreview-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      timeout: 10s
      retries: 5

  # Spring Boot 백엔드 (개발 모드)
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.dev
    container_name: bookreview-backend
    ports:
      - "8080:8080"
      - "5005:5005"  # 디버그 포트
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=bookreview
      - DB_USERNAME=bookreview_user
      - DB_PASSWORD=bookreview_password
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    volumes:
      - ./backend:/app
      - gradle_cache:/home/gradle/.gradle
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - bookreview-network
    command: ["./gradlew", "bootRun", "--debug-jvm"]

  # FastAPI AI 서비스
  ai-service:
    build:
      context: ./ai-service
      dockerfile: Dockerfile.dev
    container_name: bookreview-ai
    ports:
      - "8000:8000"
    environment:
      - ENVIRONMENT=development
      - REDIS_URL=redis://redis:6379
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./ai-service:/app
      - python_cache:/root/.cache
    depends_on:
      redis:
        condition: service_healthy
    networks:
      - bookreview-network
    command: ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]

volumes:
  mysql_data:
  redis_data:
  gradle_cache:
  python_cache:

networks:
  bookreview-network:
    driver: bridge
```

#### 2.1.2 데이터베이스 초기화 스크립트
```sql
-- /database/init/01_init.sql
CREATE DATABASE IF NOT EXISTS bookreview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bookreview_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 권한 설정
GRANT ALL PRIVILEGES ON bookreview.* TO 'bookreview_user'@'%';
GRANT ALL PRIVILEGES ON bookreview_test.* TO 'bookreview_user'@'%';
FLUSH PRIVILEGES;

-- /database/init/02_sample_data.sql
USE bookreview;

-- 샘플 사용자 데이터
INSERT INTO users (email, password, username, provider) VALUES
('admin@example.com', '$2a$10$N.9rjW/7N5VQc7GmI8G7Ke7tOlm8NjmPGgKzFW4A8N2eQ2c6z.QhO', 'admin', 'LOCAL'),
('user@example.com', '$2a$10$N.9rjW/7N5VQc7GmI8G7Ke7tOlm8NjmPGgKzFW4A8N2eQ2c6z.QhO', 'testuser', 'LOCAL');

-- 샘플 책 데이터
INSERT INTO books (title, author, publisher, category, total_pages) VALUES
('Spring Boot 실전 활용 마스터', '김영한', '인프런', 'TECHNOLOGY', 400),
('Clean Code', 'Robert C. Martin', 'Prentice Hall', 'TECHNOLOGY', 464),
('도메인 주도 설계', 'Eric Evans', '위키북스', 'TECHNOLOGY', 632);
```

### 2.2 Redis 설정

#### 2.2.1 Redis 설정 파일
```conf
# /redis/redis.conf
# 기본 설정
bind 0.0.0.0
port 6379
protected-mode no

# 메모리 설정
maxmemory 512mb
maxmemory-policy allkeys-lru

# 지속성 설정
save 900 1
save 300 10
save 60 10000

# AOF 설정
appendonly yes
appendfsync everysec

# 로그 설정
loglevel notice
logfile ""

# 클라이언트 설정
timeout 300
tcp-keepalive 300
```

### 2.3 환경 변수 관리

#### 2.3.1 개발 환경 (.env.dev)
```env
# 데이터베이스 설정
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bookreview
DB_USERNAME=bookreview_user
DB_PASSWORD=bookreview_password

# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT 설정
JWT_SECRET=your-super-secret-jwt-key-for-development
JWT_EXPIRATION=86400

# OAuth 설정
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# AI 서비스 설정
AI_SERVICE_URL=http://localhost:8000
OPENAI_API_KEY=your-openai-api-key

# 파일 업로드 설정
UPLOAD_PATH=./uploads
MAX_FILE_SIZE=10MB

# 로그 설정
LOG_LEVEL=DEBUG
LOG_FILE=./logs/application.log
```

#### 2.3.2 테스트 환경 (.env.test)
```env
# 테스트 데이터베이스
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bookreview_test
DB_USERNAME=bookreview_user
DB_PASSWORD=bookreview_password

# 테스트용 Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# 테스트용 JWT
JWT_SECRET=test-jwt-secret-key
JWT_EXPIRATION=3600

# 테스트용 AI 서비스 (Mock)
AI_SERVICE_URL=http://localhost:8001
OPENAI_API_KEY=test-api-key

# 로그 설정
LOG_LEVEL=INFO
```

### 2.4 개발 도구 설정

#### 2.4.1 IntelliJ IDEA 설정
```xml
<!-- .idea/runConfigurations/Spring_Boot_Dev.xml -->
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Spring Boot Dev" type="SpringBootApplicationConfigurationType">
    <option name="ACTIVE_PROFILES" value="dev" />
    <option name="MAIN_CLASS_NAME" value="com.bookreview.BookReviewApplication" />
    <option name="MODULE_NAME" value="bookreview-backend.main" />
    <option name="VM_PARAMETERS" value="-Xmx1024m -XX:+UseG1GC" />
    <option name="PROGRAM_PARAMETERS" value="--debug" />
    <envs>
      <env name="SPRING_PROFILES_ACTIVE" value="dev" />
    </envs>
  </configuration>
</component>
```

#### 2.4.2 VS Code 설정 (React Native)
```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Debug React Native",
      "type": "reactnative",
      "request": "launch",
      "platform": "android",
      "sourceMaps": true,
      "outDir": "${workspaceFolder}/.vscode/.react"
    },
    {
      "name": "Debug React Native Web",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/node_modules/react-scripts/bin/react-scripts.js",
      "args": ["start"],
      "env": {
        "BROWSER": "none"
      }
    }
  ]
}
```

### 2.5 CI/CD 파이프라인 준비

#### 2.5.1 GitHub Actions 워크플로우
```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test-backend:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_DATABASE: bookreview_test
          MYSQL_USER: test_user
          MYSQL_PASSWORD: test_password
          MYSQL_ROOT_PASSWORD: root_password
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3
        ports:
          - 3306:3306

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Cache Gradle dependencies
      uses: actions/cache@v3
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
    
    - name: Run tests
      run: |
        cd backend
        ./gradlew test
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Backend Tests
        path: backend/build/test-results/test/*.xml
        reporter: java-junit

  test-ai-service:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
    
    - name: Install dependencies
      run: |
        cd ai-service
        pip install -r requirements.txt
        pip install pytest pytest-cov
    
    - name: Run tests
      run: |
        cd ai-service
        pytest --cov=. --cov-report=xml
    
    - name: Upload coverage
      uses: codecov/codecov-action@v3
      with:
        file: ./ai-service/coverage.xml
        name: ai-service-coverage

  test-frontend:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json
    
    - name: Install dependencies
      run: |
        cd frontend
        npm ci
    
    - name: Run tests
      run: |
        cd frontend
        npm test -- --coverage --watchAll=false
    
    - name: Build app
      run: |
        cd frontend
        npm run build
```

### 2.6 모니터링 및 로깅 설정

#### 2.6.1 Prometheus 설정 (향후 확장)
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'fastapi'
    static_configs:
      - targets: ['ai-service:8000']
    metrics_path: '/metrics'
    
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql:9104']
```

#### 2.6.2 로그 집계 설정 (ELK Stack 준비)
```yaml
# logging/docker-compose.yml
version: '3.8'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.14.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    
  logstash:
    image: docker.elastic.co/logstash/logstash:7.14.0
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
    ports:
      - "5000:5000"
    depends_on:
      - elasticsearch
    
  kibana:
    image: docker.elastic.co/kibana/kibana:7.14.0
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on:
      - elasticsearch
```

## 3. 성능 최적화 계획

### 3.1 데이터베이스 최적화
- 쿼리 실행 계획 분석 및 최적화
- 인덱스 활용도 모니터링
- 슬로우 쿼리 로그 분석
- 커넥션 풀 최적화

### 3.2 캐시 전략
- Redis 캐시 히트율 모니터링
- 캐시 키 만료 정책 최적화
- 분산 캐시 확장 계획

### 3.3 애플리케이션 최적화
- JVM 힙 메모리 튜닝
- 가비지 컬렉션 최적화
- 스레드 풀 설정 최적화
- 비동기 처리 도입