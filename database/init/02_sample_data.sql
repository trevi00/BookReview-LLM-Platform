-- 샘플 데이터 삽입 스크립트
USE bookreview;

-- 1. 샘플 사용자 데이터 삽입
INSERT INTO users (email, password, username, provider, is_active) VALUES
-- 일반 로그인 사용자 (비밀번호: password123 - BCrypt 해시)
('admin@bookreview.com', '$2a$10$N.9rjW/7N5VQc7GmI8G7Ke7tOlm8NjmPGgKzFW4A8N2eQ2c6z.QhO', '관리자', 'LOCAL', TRUE),
('user1@example.com', '$2a$10$N.9rjW/7N5VQc7GmI8G7Ke7tOlm8NjmPGgKzFW4A8N2eQ2c6z.QhO', '독서애호가', 'LOCAL', TRUE),
('user2@example.com', '$2a$10$N.9rjW/7N5VQc7GmI8G7Ke7tOlm8NjmPGgKzFW4A8N2eQ2c6z.QhO', '책벌레', 'LOCAL', TRUE),
-- Google OAuth 사용자
('google.user@gmail.com', NULL, 'Google사용자', 'GOOGLE', TRUE);

-- 2. 샘플 책 데이터 삽입
INSERT INTO books (title, author, publisher, isbn, published_year, description, total_pages, category) VALUES
-- 기술서적
('클린 코드', '로버트 C. 마틴', '인사이트', '9788966260959', 2013, 
 '애자일 소프트웨어 장인 정신. 깨끗한 코드를 작성하는 방법에 대한 실용적인 가이드.', 
 584, 'TECHNOLOGY'),

('이펙티브 자바', '조슈아 블로크', '인사이트', '9788966262281', 2018,
 '자바 플랫폼 설계자가 알려주는 자바 프로그래밍 기법과 모범 사례.', 
 416, 'TECHNOLOGY'),

('스프링 부트 실전 활용 마스터', '그렉 턴키스트', '한빛미디어', '9791162244562', 2021,
 'Spring Boot를 활용한 실전 웹 애플리케이션 개발 가이드.',
 528, 'TECHNOLOGY'),

-- 자기계발서
('아토믹 해빗', '제임스 클리어', '비즈니스북스', '9791162540817', 2019,
 '작은 습관이 만드는 큰 변화. 습관의 과학적 원리와 실천 방법.',
 384, 'SELF_HELP'),

('데일 카네기 인간관계론', '데일 카네기', '현대지성', '9791139701845', 2019,
 '사람의 마음을 얻는 불변의 원칙들. 인간관계의 고전.',
 312, 'SELF_HELP'),

-- 소설
('1984', '조지 오웰', '민음사', '9788937460777', 2003,
 '전체주의 사회를 그린 디스토피아 소설의 걸작.',
 328, 'FICTION'),

('해리 포터와 마법사의 돌', 'J.K. 롤링', '문학수첩', '9788983920553', 2000,
 '마법사 해리 포터의 모험을 그린 판타지 소설.',
 320, 'FICTION'),

-- 역사서
('사피엔스', '유발 하라리', '김영사', '9788934972464', 2015,
 '인류의 역사를 새로운 관점에서 조명한 역사서.',
 636, 'HISTORY'),

-- 과학서
('코스모스', '칼 세이건', '사이언스북스', '9788983711892', 2006,
 '우주와 생명, 문명에 대한 경이로운 여행.',
 650, 'SCIENCE'),

-- 비즈니스
('린 스타트업', '에릭 리스', '한국경제신문', '9788947528535', 2012,
 '지속 가능한 사업을 위한 창업 방법론.',
 336, 'BUSINESS');

-- 3. 샘플 user_books 데이터 (사용자가 등록한 책들)
INSERT INTO user_books (user_id, book_id, status, start_date, current_page) VALUES
-- user1이 읽고 있는 책들
(2, 1, 'READING', '2024-01-01', 150),     -- 클린 코드 읽는 중
(2, 4, 'COMPLETED', '2023-12-01', 384),   -- 아토믹 해빗 완료
(2, 6, 'READING', '2024-01-15', 100),     -- 1984 읽는 중

-- user2가 읽고 있는 책들  
(3, 2, 'READING', '2024-01-10', 200),     -- 이펙티브 자바 읽는 중
(3, 3, 'NOT_STARTED', NULL, 0),          -- 스프링 부트 시작 전
(3, 8, 'COMPLETED', '2023-11-01', 636);  -- 사피엔스 완료

-- 4. 샘플 chapters 데이터 (목차)
INSERT INTO chapters (user_book_id, chapter_number, title, start_page, end_page, description) VALUES
-- 클린 코드 목차 (user1)
(1, 1, '깨끗한 코드', 1, 30, '깨끗한 코드란 무엇인가에 대한 기본 개념'),
(1, 2, '의미 있는 이름', 31, 50, '변수, 함수, 클래스 이름을 잘 짓는 방법'),
(1, 3, '함수', 51, 80, '좋은 함수를 작성하는 원칙들'),
(1, 4, '주석', 81, 100, '주석을 올바르게 사용하는 방법'),
(1, 5, '형식 맞추기', 101, 120, '코드 형식의 중요성과 규칙'),

-- 아토믹 해빗 목차 (user1)
(2, 1, '습관의 놀라운 힘', 1, 40, '작은 습관이 만드는 큰 변화의 원리'),
(2, 2, '뇌는 어떻게 습관을 만드는가', 41, 80, '습관 형성의 과학적 메커니즘'),
(2, 3, '좋은 습관을 만드는 4단계', 81, 150, '습관 형성의 실전 방법'),

-- 이펙티브 자바 목차 (user2)
(4, 1, '객체 생성과 파괴', 1, 50, '객체의 생성과 소멸에 관한 모범 사례'),
(4, 2, '모든 객체의 공통 메서드', 51, 100, 'Object 클래스 메서드 재정의'),
(4, 3, '클래스와 인터페이스', 101, 180, '클래스와 인터페이스 설계 원칙');

-- 5. 샘플 reading_notes 데이터 (독서 기록)
INSERT INTO reading_notes (chapter_id, user_id, content, note_type, page_number, is_private) VALUES
-- 클린 코드 독서 기록 (user1)
(1, 2, '깨끗한 코드란 다른 사람이 쉽게 읽고 이해할 수 있는 코드라는 것을 배웠다. 코드는 소통의 도구라는 점이 인상깊었다.', 
 'LEARNING', 15, FALSE),

(2, 2, '변수명을 지을 때 의도를 분명히 밝혀야 한다는 원칙이 중요하다. d보다는 daysSinceCreation이 훨씬 명확하다.', 
 'IMPRESSION', 35, FALSE),

(3, 2, '함수는 한 가지 일만 해야 한다는 SRP 원칙을 함수 레벨에서도 적용해야 한다는 것을 알았다.', 
 'LEARNING', 60, FALSE),

-- 아토믹 해빗 독서 기록 (user1)
(6, 2, '1%의 개선이 1년 후에는 37배의 성장을 만든다는 복리 효과가 놀랍다. 작은 습관의 힘을 과소평가했던 것 같다.', 
 'IMPRESSION', 25, FALSE),

(7, 2, '습관 루프: 신호 → 갈망 → 반응 → 보상. 이 4단계를 이해하면 습관을 의도적으로 설계할 수 있겠다.', 
 'LEARNING', 65, FALSE),

-- 이펙티브 자바 독서 기록 (user2)  
(9, 3, '생성자 대신 정적 팩터리 메서드를 고려하라는 항목이 흥미롭다. valueOf() 같은 메서드의 장점을 이제 이해했다.', 
 'LEARNING', 25, FALSE),

(10, 3, 'equals()를 재정의할 때는 반드시 hashCode()도 재정의해야 한다는 규칙을 잊지 말자.', 
 'QUESTION', 75, FALSE);

-- 6. 샘플 feedbacks 데이터 (AI 피드백)
INSERT INTO feedbacks (reading_note_id, content, feedback_type, ai_model, is_useful, user_rating) VALUES
(1, '훌륭한 통찰입니다! 클린 코드의 핵심을 잘 이해하셨군요. 코드가 소통의 도구라는 관점에서, 실제 프로젝트에서는 어떤 상황에서 이 원칙이 가장 중요하다고 생각하시나요? 팀원들과의 코드 리뷰나 유지보수 경험이 있다면 구체적인 예시를 생각해보시는 것도 좋겠습니다.', 
 'QUESTION', 'gpt-4', TRUE, 5),

(2, '정확한 관찰입니다! 의미 있는 이름짓기는 정말 중요한 스킬이에요. 더 나아가서, 도메인 특화 언어(Domain-Specific Language)를 사용하는 것도 고려해보세요. 예를 들어, 은행 시스템에서는 amount보다는 depositAmount, withdrawalAmount처럼 도메인 맥락을 반영하는 이름이 더 명확할 수 있습니다.', 
 'SUGGESTION', 'gpt-4', TRUE, 4),

(3, '단일 책임 원칙(SRP)을 함수 레벨에서 적용하는 것, 정말 중요한 포인트입니다! 이를 실제로 적용할 때 함수 길이는 어떻게 관리하고 계신가요? Robert Martin은 함수가 20줄을 넘지 않는 것을 권장하는데, 이에 대한 생각은 어떠신지요?', 
 'COMMENT', 'gpt-4', TRUE, 5);

-- 7. 샘플 reading_goals 데이터 (독서 목표)
INSERT INTO reading_goals (user_id, year, target_books, target_pages, current_books, current_pages) VALUES
(2, 2024, 24, 6000, 2, 534),  -- user1의 2024년 목표
(3, 2024, 12, 3600, 1, 636);  -- user2의 2024년 목표

-- 8. 샘플 reading_sessions 데이터 (독서 세션)
INSERT INTO reading_sessions (user_book_id, start_time, end_time, pages_read, notes) VALUES
-- user1의 독서 세션들
(1, '2024-01-01 09:00:00', '2024-01-01 10:30:00', 30, '아침 독서. 집중도 좋았음'),
(1, '2024-01-02 14:00:00', '2024-01-02 15:00:00', 20, '점심시간 독서'),
(1, '2024-01-03 20:00:00', '2024-01-03 21:30:00', 25, '저녁 독서. 함수 부분이 어려웠음'),

-- user2의 독서 세션들  
(4, '2024-01-10 10:00:00', '2024-01-10 12:00:00', 50, '주말 오전 집중 독서'),
(4, '2024-01-11 19:00:00', '2024-01-11 20:30:00', 30, '퇴근 후 독서');

-- 완료 메시지
SELECT 'Sample data insertion completed successfully!' as message;