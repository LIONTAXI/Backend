package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.dto.SettlementDto;
import taxi.tago.entity.*;
import taxi.tago.repository.*;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 정산 관련 비즈니스 로직을 담당하는 서비스 클래스
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final TaxiPartyRepository taxiPartyRepository;
    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;
    private final NotificationService notificationService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 정산 생성 메서드
    @Transactional
    public Long createSettlement(Long hostId, SettlementDto.CreateRequest request) {
        // 택시팟 조회
        TaxiParty taxiParty = taxiPartyRepository.findById(request.getTaxiPartyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 택시팟이 존재하지 않습니다. id = " + request.getTaxiPartyId()
                ));

        // 총대슈니 조회
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. id = " + hostId
                ));

        // 권한 체크: 현재 로그인한 유저가 이 택시팟의 총대인지 확인
        if (!taxiParty.getUser().getId().equals(hostId)) {
            throw new AccessDeniedException("총대슈니만 정산을 생성할 수 있습니다.");
        }

        // 이미 정산이 생성되어 있는지 체크 (택시팟과 1:1 관계이므로)
        settlementRepository.findByTaxiParty(taxiParty).ifPresent(s -> {
            throw new IllegalArgumentException("이미 정산이 생성된 택시팟입니다. settlementId = " + s.getId());
        });

        // Settlement 엔티티 생성
        Settlement settlement = Settlement.create(
                taxiParty,
                host,
                request.getTotalFare(),
                request.getBankName(),
                request.getAccountNumber()
        );

        // 참여자별 엔티티 생성
        for (SettlementDto.ParticipantShare share : request.getParticipants()) {
            User participantUser = userRepository.findById(share.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "정산 대상 사용자를 찾을 수 없습니다. id = " + share.getUserId()
                    ));

            // 총대인지 여부 확인
            boolean isHost = participantUser.getId().equals(hostId);

            SettlementParticipant participant = new SettlementParticipant(
                    participantUser,
                    share.getAmount(),
                    isHost
            );

            // 총대는 이미 본인의 택시비를 지불한 상태이므로 바로 납부 완료 처리
            if (isHost) {
                participant.markPaid();
            }

            // Settlement에 참여자 추가 (양방향 연관관계 정리)
            settlement.addParticipant(participant);
        }

        // DB에 저장 (Cascade 처리로 participant도 함께 INSERT)
        Settlement saved = settlementRepository.save(settlement);

        // 총대를 제외한 참여자들에게 정산 요청 알림 발송 (요청 알림과 동일한 방식으로 처리)
        String hostName = host.getName() != null ? host.getName() : "총대슈니";
        saved.getParticipants().stream()
                .filter(p -> !p.getUser().getId().equals(hostId)) // 총대 제외
                .forEach(p -> {
                    try {
                        notificationService.sendSettlementRequest(
                                p.getUser().getId(),
                                saved.getId(),
                                hostName
                        );
                        log.debug("정산 요청 알림 전송 성공: receiverId={}, settlementId={}", 
                                p.getUser().getId(), saved.getId());
                    } catch (Exception e) {
                        log.error("정산 요청 알림 전송 중 오류 발생 (정산 생성은 성공): receiverId={}, settlementId={}, error={}", 
                                p.getUser().getId(), saved.getId(), e.getMessage(), e);
                        // 알림 실패해도 정산 생성은 성공 처리
                    }
                });

        // 채팅방에 정산 안내 메시지 자동 전송
        sendSettlementChatMessage(saved, host, false); // false = 최초 생성용 메시지

        log.info("정산 생성 완료: settlementId={}, taxiPartyId={}, hostId={}",
                saved.getId(), taxiParty.getId(), hostId);

        return saved.getId();
    }

    // 정산 상세 조회 (총대 또는 정산 참여자만 조회 가능)
    @Transactional(readOnly = true)
    public SettlementDto.DetailResponse getSettlementDetail(Long settlementId, Long userId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "정산 내역을 찾을 수 없습니다. id = " + settlementId
                ));
        // 본인이 이 정산에 관련된 사람인지 체크
        boolean isRelated = settlement.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isRelated) {
            throw new AccessDeniedException("해당 정산을 조회할 권한이 없습니다.");
        }

        // 참여자 정보 -> DTO로 변환
        List<SettlementDto.ParticipantResponse> participants = settlement.getParticipants().stream()
                .map(p -> new SettlementDto.ParticipantResponse(
                        p.getUser().getId(),
                        p.getUser().getName(),
                        p.getUser().getShortStudentId(),
                        p.getUser().getImgUrl(),
                        p.getAmount(),
                        p.isPaid(),
                        p.getPaidAt(),
                        p.isHost()
                ))
                .collect(Collectors.toList());

        // 상단 정산 정보 + 참여자 리스트를 한 DTO에 담아서 반환
        return new SettlementDto.DetailResponse(
                settlement.getId(),
                settlement.getTaxiParty().getId(),
                settlement.getTotalFare(),
                settlement.getBankName(),
                settlement.getAccountNumber(),
                settlement.getStatus().name(),
                settlement.getCreatedAt(),
                participants
        );
    }

    // 총대가 특정 참여자의 정산 상태를 "정산 완료"로 표시하는 메서드
    @Transactional
    public void markPaid(Long settlementId, Long hostId, Long targetUserId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "정산 내역을 찾을 수 없습니다. id = " + settlementId
                ));

        // 요청자가 이 정산의 총대인지 확인
        if (!settlement.getHost().getId().equals(hostId)) {
            throw new AccessDeniedException("총대슈니만 정산 완료 처리를 할 수 있습니다.");
        }

        // 대상 사용자가 이 정산에 포함된 사람인지 확인
        SettlementParticipant participant = settlementParticipantRepository
                .findBySettlementIdAndUserId(settlementId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 사용자는 이 정산 대상에 포함되어 있지 않습니다."
                ));

        // 이미 paid == true라면 markPaid 내부에서 아무 일도 하지 않음
        participant.markPaid();

        // 모든 인원이 납부 완료되었는지 검사 후, 완료면 Settlement 상태 변경
        settlement.updateStatusIfCompleted();

        log.info("정산 납부 완료처리: settlementId={}, hostId={}, targetUserId={}",
                settlementId, hostId, targetUserId);
    }

    // 정산 재촉하기 메서드 (알림 + 채팅 재전송) (2시간에 한 번씩만)
    @Transactional
    public void remindUnpaid(Long settlementId, Long hostId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "정산 내역을 찾을 수 없습니다. id = " + settlementId
                ));

        // 요청자가 이 정산의 총대인지 확인
        if (!settlement.getHost().getId().equals(hostId)) {
            throw new AccessDeniedException("총대슈니만 정산 재촉을 할 수 있습니다.");
        }

        // 2시간 이내에 이미 재촉한 적이 있는지 검사
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastRemindedAt = settlement.getLastRemindedAt();

        // 마지막 재촉 시각으로부터 2시간이 지나지 않았다면
        if (lastRemindedAt != null && lastRemindedAt.isAfter(now.minusHours(2))) {
            throw new IllegalArgumentException("정산 재촉 알림은 2시간에 한 번만 보낼 수 있습니다.");
        }

        String hostName = settlement.getHost().getName() != null
                ? settlement.getHost().getName()
                : "총대슈니";

        // 아직 납부하지 않은 참여자들에게만 재촉 알림 발송 (총대 본인 제외) (요청 알림과 동일한 방식으로 처리)
        settlement.getParticipants().stream()
                .filter(p -> !p.isPaid()) // 미납자만
                .filter(p -> !p.getUser().getId().equals(hostId)) // 총대 제외
                .forEach(p -> {
                    try {
                        notificationService.sendSettlementRemind(
                                p.getUser().getId(),
                                settlement.getId(),
                                hostName
                        );
                        log.debug("정산 재촉 알림 전송 성공: receiverId={}, settlementId={}", 
                                p.getUser().getId(), settlement.getId());
                    } catch (Exception e) {
                        log.error("정산 재촉 알림 전송 중 오류 발생 (재촉은 성공): receiverId={}, settlementId={}, error={}", 
                                p.getUser().getId(), settlement.getId(), e.getMessage(), e);
                        // 알림 실패해도 재촉은 성공 처리
                    }
                });

        // 채팅방에도 정산 안내 메시지를 다시 전송 (재촉용)
        sendSettlementChatMessage(settlement, settlement.getHost(), true); // true = 재촉용 메시지

        // 마지막 재촉 시각 업데이트
        settlement.updateLastRemindedAt(now);

        log.info("정산 재촉 알림 전송: settlementId={}, hostId={}", settlementId, hostId);
    }


    // 정산 관련 채팅 메시지를 채팅방에 전송하는 메서드
    // 최초 생성 시: "슈니은행 123-456-7890으로 1,234원씩 입금 부탁드립니다!"
    // 재촉 시: "아직 정산하지 않으신 슈니는 123-456-7890으로 1,234원씩 입금 부탁드립니다!"
    private void sendSettlementChatMessage(Settlement settlement, User host, boolean reminder) {
        // 택시팟에 연결된 채팅방 찾기
        chatRoomRepository.findByTaxiPartyId(settlement.getTaxiParty().getId())
                .ifPresent(chatRoom -> {
                    if (chatRoom.isClosed()) {
                        // 이미 종료된 채팅방이면 메시지 전송 X
                        return;
                    }

                    // 참여자들의 금액이 모두 동일한지 확인
                    Integer uniformAmount = calculateUniformAmount(settlement);

                    String bankInfo = settlement.getBankName() + " " + settlement.getAccountNumber();
                    String messageContent;

                    if (uniformAmount != null) {
                        if (!reminder) {
                            // 최초 정산 생성 시 메시지
                            messageContent = String.format(
                                    "%s으로 %d원씩 입금 부탁드립니다!",
                                    bankInfo,
                                    uniformAmount
                            );
                        } else {
                            // 재촉 메시지
                            messageContent = String.format(
                                    "아직 정산하지 않으신 슈니는 %s으로 %d원씩 입금 부탁드립니다!",
                                    bankInfo,
                                    uniformAmount
                            );
                        }
                    } else {
                        // 금액이 사람마다 다를 경우
                        if (!reminder) {
                            messageContent = String.format(
                                    "%s으로 각자 앱에 표시된 정산 금액 입금 부탁드립니다!",
                                    bankInfo
                            );
                        } else {
                            // 재촉 메시지
                            messageContent = String.format(
                                    "아직 정산하지 않으신 슈니는 %s으로 앱에 표시된 금액 입금 부탁드립니다!",
                                    bankInfo
                            );
                        }
                    }

                    // ChatMEssage 엔티티 생성
                    ChatMessage message = ChatMessage.createTextMessage(
                            chatRoom,
                            host,
                            messageContent
                    );

                    // 채팅방 최근 메시지 정보 갱신
                    LocalDateTime now = LocalDateTime.now();
                    chatRoom.updateMessage(messageContent, now);

                    // DB에 메시지 저장
                    chatMessageRepository.save(message);
                });
    }

    // 모든 참여자의 amount가 동일한지 검사하는 메서드
    // 모두 동일하면 그 금액을 반환하고, 하나라도 다르면 null을 반환함
    private Integer calculateUniformAmount(Settlement settlement) {
        return settlement.getParticipants().stream()
                .map(SettlementParticipant::getAmount)
                .distinct()
                .reduce((a, b) -> null) // 서로 다른 값이 두 개 이상이면 null
                .orElseGet(() -> settlement.getParticipants().isEmpty()
                        ? null : settlement.getParticipants().get(0).getAmount());
    }

    // 이미 생성되어 있는 정산에 대해, "현재 로그인한 유저가 속해있는 settlementId"를 조회하는 메서드
    @Transactional(readOnly = true)
    public SettlementDto.SettlementResponse getMySettlementId(Long taxiPartyId, Long userId) {
        // 택시팟 존재 여부 확인
        TaxiParty taxiParty = taxiPartyRepository.findById(taxiPartyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 택시팟을 찾을 수 없습니다. id = " + taxiPartyId
                ));

        // 해당 택시팟에 정산이 생성되어 있는지 조회
        return settlementRepository.findByTaxiParty(taxiParty)
                .map(settlement -> {
                    // 정산은 있는 상태에서, 현재 유저가 이 정산의 구성원인지 검사
                    boolean isHost = settlement.getHost().getId().equals(userId);

                    boolean isParticipant = settlement.getParticipants().stream()
                            .anyMatch(p -> p.getUser().equals(userId));

                    if (!isHost && !isParticipant) {
                        // 정산은 있지만 이 유저는 관련이 없는 경우 -> 권한 없음
                        throw new IllegalArgumentException("해당 정산에 참여한 슈니만 settlementId를 조회할 수 있습니다.");
                    }

                    // 정산이 존재하며 유저도 해당 택시팟의 멤버가 맞는 경우 반환
                    return new SettlementDto.SettlementResponse(true, settlement.getId());
                })
                .orElseGet(() ->
                        // 아직 이 택시팟에 대해 정산이 생성되지 않은 경우 -> hasSettlement = false, settlementId = null
                        new SettlementDto.SettlementResponse(false, null)
                );
    }
}
