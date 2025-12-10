package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import taxi.tago.entity.Notification;

import java.time.LocalDateTime;

// 알림 응답 DTO - 프론트엔드로 전달되는 알림 데이터 (UI에서 알림 카드를 렌더링하는 데 필요한 모든 정보 포함)
@Getter
@AllArgsConstructor
public class NotificationDto {

    // 알림 ID
    private Long id;

    // 알림 제목 (예: "정산요청이 들어왔어요.")
    private String title;

    // 알림 본문 (예: "빠른 시일 내에 정산해 주세요.")
    private String body;

    // 알림 유형 (SETTLEMENT_REQUEST, REVIEW_ARRIVED 등)
    private String type;

    // 클릭 시 이동할 화면 타입 (SETTLEMENT, REVIEW, TAXI_ROOM 등)
    private String targetType;

    // 클릭 시 이동할 화면의 ID
    private Long targetId;

    // 읽음 여부 (false: 안 읽은 알림, true: 읽은 알림)
    private boolean read;

    // 생성 시각 (프론트에서 "11:44" 또는 "10/31" 형식으로 표시)
    private LocalDateTime createdAt;

    // Notification 엔티티를 NotificationDto로 변환
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getType().name(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

