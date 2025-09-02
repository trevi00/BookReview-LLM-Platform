-- 데이터베이스 초기화 스크립트
-- UTF-8 인코딩 설정
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 데이터베이스 생성 (이미 존재하는 경우 무시)
CREATE DATABASE IF NOT EXISTS bookreview 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 테스트용 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS bookreview_test 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 사용자 권한 설정
-- 메인 데이터베이스
GRANT ALL PRIVILEGES ON bookreview.* TO 'bookreview_user'@'%';

-- 테스트 데이터베이스
GRANT ALL PRIVILEGES ON bookreview_test.* TO 'bookreview_user'@'%';

-- 권한 적용
FLUSH PRIVILEGES;

-- bookreview 데이터베이스 사용
USE bookreview;

-- 테이블 생성 (물리적 설계 문서 기반)

-- 1. users 테이블
CREATE TABLE IF NOT EXISTS users (
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
    INDEX idx_users_created_at (created_at),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. books 테이블
CREATE TABLE IF NOT EXISTS books (
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
    
    INDEX idx_books_title (title(255)),
    INDEX idx_books_author (author),
    INDEX idx_books_isbn (isbn),
    INDEX idx_books_category (category),
    INDEX idx_books_created_at (created_at),
    FULLTEXT idx_books_search (title, author, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. user_books 테이블 (사용자-책 연결)
CREATE TABLE IF NOT EXISTS user_books (
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
    INDEX idx_user_books_user_status (user_id, status),
    UNIQUE KEY uk_user_book_reading (user_id, book_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. chapters 테이블
CREATE TABLE IF NOT EXISTS chapters (
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
    UNIQUE KEY uk_chapter_number_per_book (user_book_id, chapter_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. reading_notes 테이블
CREATE TABLE IF NOT EXISTS reading_notes (
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
    INDEX idx_reading_notes_user_type_date (user_id, note_type, created_at),
    FULLTEXT idx_reading_notes_content (content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. feedbacks 테이블
CREATE TABLE IF NOT EXISTS feedbacks (
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
    INDEX idx_feedbacks_created_at (created_at),
    INDEX idx_feedbacks_useful (is_useful)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. reading_goals 테이블
CREATE TABLE IF NOT EXISTS reading_goals (
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
    UNIQUE KEY uk_user_year_goal (user_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. reading_sessions 테이블
CREATE TABLE IF NOT EXISTS reading_sessions (
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
    INDEX idx_reading_sessions_date ((DATE(start_time)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;