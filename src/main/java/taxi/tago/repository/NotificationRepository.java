package taxi.tago.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.Notification;

// 알림 Repository - 알림 조회 및 통계 쿼리를 제공
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 사용자의 알림 목록을 생성일시 내림차순으로 조회 (최신 알림이 먼저 오도록)
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // 특정 사용자의 미확인(안 읽은) 알림 개수를 조회 (벨 아이콘 배지에 표시할 숫자)
    long countByReceiverIdAndReadFalse(Long receiverId);
}

