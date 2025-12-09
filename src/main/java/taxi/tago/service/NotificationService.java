package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.NotificationType;
import taxi.tago.dto.NotificationDto;
import taxi.tago.entity.Notification;
import taxi.tago.entity.User;
import taxi.tago.repository.NotificationRepository;
import taxi.tago.repository.UserRepository;
import taxi.tago.util.SseEmitters;
import org.springframework.transaction.annotation.Propagation;

/**
 * 알림 서비스
 * 
 * 알림 생성, 조회, 읽음 처리 등의 비즈니스 로직을 담당합니다.
 * 
 * 핵심 개념:
 * 1. 알림은 도메인 이벤트(정산요청, 참여수락 등) 발생 시 생성됩니다.
 * 2. 각 알림 타입별로 전용 메서드를 제공합니다.
 * 3. 알림 생성 시 targetType과 targetId를 저장하여 프론트에서 라우팅 가능하게 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitters sseEmitters;

    /**
     * 알림 목록 조회
     * 
     * @param receiverId 알림을 받은 사용자 ID
     * @param pageable 페이지네이션 정보
     * @return 알림 목록 (최신순 정렬)
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long receiverId, Pageable pageable) {
        return notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(receiverId, pageable)
                .map(NotificationDto::from);
    }

    /**
     * 미확인 알림 개수 조회
     * 벨 아이콘 배지에 표시할 숫자입니다.
     * 
     * @param receiverId 알림을 받은 사용자 ID
     * @return 미확인 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long receiverId) {
        return notificationRepository.countByReceiverIdAndReadFalse(receiverId);
    }

    /**
     * 알림 읽음 처리
     * 
     * 사용자가 알림 카드를 클릭하면 호출됩니다.
     * 
     * @param notificationId 알림 ID
     * @param userId 현재 사용자 ID (본인 알림만 읽음 처리 가능)
     * @throws AccessDeniedException 본인 알림이 아닐 경우
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인 알림인지 확인
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedException("본인 알림만 읽음 처리할 수 있습니다.");
        }

        // 이미 읽은 알림이면 처리하지 않음
        if (!notification.isRead()) {
            notification.markAsRead();
            log.info("알림 읽음 처리 완료: notificationId={}, userId={}", notificationId, userId);
        }
    }

    // ==================== 알림 생성 메서드들 ====================
    // 각 도메인 이벤트 발생 시 호출되는 메서드들입니다.

    /**
     * 정산요청 알림 생성
     * 
     * 정산요청이 발생했을 때 호출됩니다.
     * 
     * @param receiverId 알림을 받을 사용자 ID
     * @param settlementId 정산 ID (클릭 시 이동할 정산 상세 페이지)
     * @param requesterName 정산을 요청한 사용자 이름 (선택사항)
     */
    @Transactional
    public void sendSettlementRequest(Long receiverId, Long settlementId, String requesterName) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = new Notification(
                receiver,
                "정산요청이 들어왔어요.",
                "빠른 시일 내에 정산해 주세요.",
                NotificationType.SETTLEMENT_REQUEST,
                "SETTLEMENT",
                settlementId
        );

        Notification saved = notificationRepository.save(notification);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
        
        log.info("정산요청 알림 생성: receiverId={}, settlementId={}", receiverId, settlementId);
    }

    /**
     * 정산 재촉 알림 생성
     * 
     * 정산이 지연되어 재촉할 때 호출됩니다.
     * 
     * @param receiverId 알림을 받을 사용자 ID
     * @param settlementId 정산 ID
     * @param requesterName 정산을 재촉한 사용자 이름
     */
    @Transactional
    public void sendSettlementRemind(Long receiverId, Long settlementId, String requesterName) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = new Notification(
                receiver,
                requesterName + "님이 정산을 재촉했어요.",
                "프로필에 미정산 이력이 남아요. 정산을 서둘러 주세요.",
                NotificationType.SETTLEMENT_REMIND,
                "SETTLEMENT",
                settlementId
        );

        Notification saved = notificationRepository.save(notification);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
        
        log.info("정산 재촉 알림 생성: receiverId={}, settlementId={}, requesterName={}", 
                receiverId, settlementId, requesterName);
    }

    /**
     * 후기 도착 알림 생성
     * 
     * 후기가 작성되어 도착했을 때 호출됩니다.
     * 
     * @param receiverId 알림을 받을 사용자 ID
     * @param reviewId 후기 ID (클릭 시 이동할 후기 상세 페이지)
     */
    @Transactional
    public void sendReviewArrived(Long receiverId, Long reviewId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = new Notification(
                receiver,
                "후기가 도착했어요.",
                "어떤 후기가 도착했는지 확인해 보세요.",
                NotificationType.REVIEW_ARRIVED,
                "REVIEW",
                reviewId
        );

        Notification saved = notificationRepository.save(notification);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
        
        log.info("후기 도착 알림 생성: receiverId={}, reviewId={}", receiverId, reviewId);
    }

    /**
     * 택시팟 참여 요청 알림 생성
     * 
     * 동승슈니가 택시팟에 참여 요청을 보냈을 때 총대에게 알림을 보냅니다.
     * 
     * @param receiverId 알림을 받을 사용자 ID (총대슈니)
     * @param taxiPartyId 택시팟 ID (클릭 시 이동할 택시팟 상세 페이지)
     * @param requesterName 참여 요청을 보낸 사용자 이름
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendTaxiParticipationRequest(Long receiverId, Long taxiPartyId, String requesterName) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = new Notification(
                receiver,
                "택시팟 참여 요청이 왔어요.",
                requesterName + "님이 같이 타기를 요청했어요.",
                NotificationType.TAXI_PARTICIPATION_REQUEST,
                "TAXI_PARTY",
                taxiPartyId
        );

        Notification saved = notificationRepository.save(notification);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
        
        log.info("택시팟 참여 요청 알림 생성: receiverId={}, taxiPartyId={}, requesterName={}", 
                receiverId, taxiPartyId, requesterName);
    }

    /**
     * 택시팟 참여 수락 알림 생성
     * 
     * 택시팟 참여 요청이 수락되었을 때 호출됩니다.
     * 
     * @param receiverId 알림을 받을 사용자 ID (참여 요청을 보낸 사람)
     * @param roomId 채팅방 ID (클릭 시 이동할 채팅방)
     * @param hostName 총대슈니 이름 (참여를 수락한 사람)
     */
    @Transactional
    public void sendTaxiParticipationAccepted(Long receiverId, Long roomId, String hostName) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = new Notification(
                receiver,
                hostName + "님이 택시팟 참여를 수락했어요.",
                "어서 채팅방으로 들어가 소통해 보세요.",
                NotificationType.TAXI_PARTICIPATION_ACCEPTED,
                "TAXI_ROOM",
                roomId
        );

        Notification saved = notificationRepository.save(notification);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
        
        log.info("택시팟 참여 수락 알림 생성: receiverId={}, roomId={}, hostName={}", 
                receiverId, roomId, hostName);
    }
}

