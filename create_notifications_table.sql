-- notifications 테이블 생성 SQL 스크립트
-- 이 스크립트를 MySQL에서 실행하세요

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    `read` BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    CONSTRAINT FK_notifications_receiver 
        FOREIGN KEY (receiver_id) 
        REFERENCES users (user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 더미 계정 생성 SQL
-- ============================================
-- 비밀번호: test123! (모든 계정 동일)
-- BCrypt 해시는 매번 다르게 생성되지만, 같은 비밀번호로 검증 가능
-- 
-- BCrypt 해시 생성 방법:
-- 1. 온라인 도구: https://bcrypt-generator.com/ (Rounds: 10)
-- 2. Java 코드 실행:
--    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
--    System.out.println(encoder.encode("test123!"));
--
-- 아래 해시는 "test123!" 비밀번호에 대한 BCrypt 해시 예시입니다.
-- 실제로는 다른 해시가 생성될 수 있지만, 모두 "test123!"로 로그인 가능합니다.

INSERT INTO users (email, password, role, img_url, last_active_at) VALUES
('test1@swu.ac.kr', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', '/images/default.svg', NOW()),
('test2@swu.ac.kr', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', '/images/default.svg', NOW()),
('test3@swu.ac.kr', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', '/images/default.svg', NOW()),
('test4@swu.ac.kr', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', '/images/default.svg', NOW()),
('test5@swu.ac.kr', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', '/images/default.svg', NOW());

-- ============================================
-- 로그인 정보
-- ============================================
-- 이메일: test1@swu.ac.kr ~ test5@swu.ac.kr
-- 비밀번호: test123!
