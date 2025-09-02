package com.bookreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 책 독후감 및 AI 피드백 플랫폼 메인 애플리케이션 클래스
 * 
 * @author BookReview Team
 * @version 1.0.0
 * @since 2024-01-20
 */
@SpringBootApplication
@EnableJpaAuditing
public class BookReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookReviewApplication.class, args);
    }
}