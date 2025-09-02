-- BookReview Platform Database Schema
-- MySQL 8.0.x compatible

-- 데이터베이스 생성 (Docker에서 이미 생성되었지만 참고용)
-- CREATE DATABASE IF NOT EXISTS bookreview CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255),
    provider ENUM('LOCAL', 'GOOGLE', 'GITHUB') DEFAULT 'LOCAL',
    provider_id VARCHAR(100),
    profile_image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_provider_id (provider, provider_id),
    INDEX idx_active (is_active)
) ENGINE=InnoDB;

-- 책 테이블
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    author VARCHAR(200) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    publisher VARCHAR(100),
    published_date DATE,
    page_count INT,
    description TEXT,
    cover_image_url VARCHAR(500),
    category VARCHAR(50),
    language VARCHAR(10) DEFAULT 'ko',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_title (title),
    INDEX idx_author (author),
    INDEX idx_isbn (isbn),
    INDEX idx_category (category),
    FULLTEXT idx_search (title, author, description)
) ENGINE=InnoDB;

-- 사용자-책 연관 테이블 (내 서재)
CREATE TABLE IF NOT EXISTS user_books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    status ENUM('WANT_TO_READ', 'READING', 'COMPLETED', 'PAUSED', 'DROPPED') DEFAULT 'WANT_TO_READ',
    rating TINYINT CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_book (user_id, book_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_book_rating (book_id, rating)
) ENGINE=InnoDB;

-- 챕터 테이블
CREATE TABLE IF NOT EXISTS chapters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    chapter_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT,
    page_start INT,
    page_end INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    UNIQUE KEY uk_book_chapter (book_id, chapter_number),
    INDEX idx_book_chapter_num (book_id, chapter_number)
) ENGINE=InnoDB;

-- 독서 노트 테이블
CREATE TABLE IF NOT EXISTS reading_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    chapter_id BIGINT,
    title VARCHAR(200),
    content TEXT NOT NULL,
    note_type ENUM('SUMMARY', 'REFLECTION', 'QUESTION', 'QUOTE', 'OTHER') DEFAULT 'REFLECTION',
    page_number INT,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE SET NULL,
    INDEX idx_user_book (user_id, book_id),
    INDEX idx_book_chapter (book_id, chapter_id),
    INDEX idx_note_type (note_type),
    INDEX idx_public (is_public),
    FULLTEXT idx_content_search (title, content)
) ENGINE=InnoDB;

-- AI 피드백 테이블
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reading_note_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    feedback_type ENUM('SUMMARY_FEEDBACK', 'WRITING_IMPROVEMENT', 'CRITICAL_THINKING', 'QUESTION_SUGGESTION') DEFAULT 'SUMMARY_FEEDBACK',
    ai_model VARCHAR(50) DEFAULT 'gpt-3.5-turbo',
    tokens_used INT,
    processing_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reading_note_id) REFERENCES reading_notes(id) ON DELETE CASCADE,
    INDEX idx_note_feedback (reading_note_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

-- 독서 목표 테이블
CREATE TABLE IF NOT EXISTS reading_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goal_type ENUM('BOOKS_PER_YEAR', 'BOOKS_PER_MONTH', 'PAGES_PER_DAY', 'MINUTES_PER_DAY') NOT NULL,
    target_value INT NOT NULL,
    current_value INT DEFAULT 0,
    year INT NOT NULL,
    month INT,
    is_achieved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_goal_period (user_id, goal_type, year, month),
    INDEX idx_user_year (user_id, year),
    INDEX idx_goal_type (goal_type)
) ENGINE=InnoDB;

-- 독서 세션 테이블
CREATE TABLE IF NOT EXISTS reading_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    duration_minutes INT,
    pages_read INT DEFAULT 0,
    session_type ENUM('READING', 'NOTE_TAKING', 'REVIEW') DEFAULT 'READING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    INDEX idx_user_book_session (user_id, book_id),
    INDEX idx_session_date (started_at),
    INDEX idx_session_type (session_type)
) ENGINE=InnoDB;