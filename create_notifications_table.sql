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

