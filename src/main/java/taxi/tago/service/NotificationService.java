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

// 알림 서비스 - 알림 생성, 조회, 읽음 처리 등의 비즈니스 로직을 담당
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitters sseEmitters;

    // 알림 목록 조회 (최신순 정렬)
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long receiverId, Pageable pageable) {
        return notificationRepository
                .findByReceiverIdOrderByCreatedAtDesc(receiverId, pageable)
                .map(NotificationDto::from);
    }

    // 미확인 알림 개수 조회 (벨 아이콘 배지에 표시할 숫자)
    @Transactional(readOnly = true)
    public long getUnreadCount(Long receiverId) {
        return notificationRepository.countByReceiverIdAndReadFalse(receiverId);
    }

    // 알림 읽음 처리 (사용자가 알림 카드를 클릭하면 호출)
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

    // 정산요청 알림 생성 (정산요청이 발생했을 때 호출)
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
        log.info("정산요청 알림 DB 저장 완료: notificationId={}, receiverId={}, settlementId={}", 
                saved.getId(), receiverId, settlementId);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
    }

    // 정산 재촉 알림 생성 (정산이 지연되어 재촉할 때 호출)
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
        log.info("정산 재촉 알림 DB 저장 완료: notificationId={}, receiverId={}, settlementId={}, requesterName={}", 
                saved.getId(), receiverId, settlementId, requesterName);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
    }

    // 후기 도착 알림 생성 (후기가 작성되어 도착했을 때 호출)
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
        log.info("후기 도착 알림 DB 저장 완료: notificationId={}, receiverId={}, reviewId={}", 
                saved.getId(), receiverId, reviewId);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
    }

    // 택시팟 참여 요청 알림 생성 (동승슈니가 택시팟에 참여 요청을 보냈을 때 총대에게 알림)
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
        log.info("택시팟 참여 요청 알림 DB 저장 완료: notificationId={}, receiverId={}, taxiPartyId={}, requesterName={}", 
                saved.getId(), receiverId, taxiPartyId, requesterName);
        
        // SSE로 실시간 알림 전송 (실패해도 알림은 이미 DB에 저장됨)
        try {
            sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
            log.debug("택시팟 참여 요청 알림 SSE 전송 성공: receiverId={}, notificationId={}", 
                    receiverId, saved.getId());
        } catch (Exception e) {
            log.warn("택시팟 참여 요청 알림 SSE 전송 실패 (DB 저장은 완료): receiverId={}, notificationId={}, error={}", 
                    receiverId, saved.getId(), e.getMessage());
        }
    }

    // 택시팟 참여 수락 알림 생성 (택시팟 참여 요청이 수락되었을 때 호출)
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
        log.info("택시팟 참여 수락 알림 DB 저장 완료: notificationId={}, receiverId={}, roomId={}, hostName={}", 
                saved.getId(), receiverId, roomId, hostName);
        
        // SSE로 실시간 알림 전송
        sseEmitters.sendToUser(receiverId, "notification", NotificationDto.from(saved));
    }
}

