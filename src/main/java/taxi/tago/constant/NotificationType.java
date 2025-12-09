package taxi.tago.constant;

/**
 * 알림 타입 Enum
 * 
 * SETTLEMENT_REQUEST: 정산요청이 들어왔어요
 * SETTLEMENT_REMIND: 정산을 재촉했어요
 * REVIEW_ARRIVED: 후기가 도착했어요
 * TAXI_PARTICIPATION_REQUEST: 택시팟 참여 요청이 왔어요
 * TAXI_PARTICIPATION_ACCEPTED: 택시팟 참여를 수락했어요
 */
public enum NotificationType {
    SETTLEMENT_REQUEST,
    SETTLEMENT_REMIND,
    REVIEW_ARRIVED,
    TAXI_PARTICIPATION_REQUEST,
    TAXI_PARTICIPATION_ACCEPTED
}

