package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import taxi.tago.constant.NotificationType;

import java.time.LocalDateTime;

// 알림 엔티티 - 웹앱 내부 알림 센터에 표시되는 알림 데이터를 저장
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 알림을 받는 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // 알림 제목 (예: "정산요청이 들어왔어요.")
    @Column(nullable = false, length = 100)
    private String title;

    // 알림 본문 (예: "빠른 시일 내에 정산해 주세요.")
    @Column(length = 500)
    private String body;

    // 알림 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    // 클릭 시 이동할 화면 타입 (예: "SETTLEMENT", "REVIEW", "TAXI_ROOM")
    @Column(name = "target_type", length = 50)
    private String targetType;

    // 클릭 시 이동할 화면의 ID (예: settlementId, reviewId, roomId)
    @Column(name = "target_id")
    private Long targetId;

    // 읽음 여부 (false: 안 읽은 알림, true: 읽은 알림)
    @Column(name = "`read`", nullable = false)
    private boolean read = false;

    // 생성 시각
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 읽음 처리 시각
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 엔티티 저장 전 createdAt 자동 설정
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 알림을 읽음 처리 (read = true로 변경하고 readAt을 현재 시각으로 설정)
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    // 알림 생성용 생성자
    public Notification(User receiver, String title, String body, NotificationType type, 
                       String targetType, Long targetId) {
        this.receiver = receiver;
        this.title = title;
        this.body = body;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }
}

