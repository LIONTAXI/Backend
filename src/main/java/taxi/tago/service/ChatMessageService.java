package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.dto.chat.ChatMessageResponse;
import taxi.tago.dto.chat.ChatMessageSendRequest;
import taxi.tago.entity.*;
import taxi.tago.repository.ChatMessageRepository;
import taxi.tago.repository.ChatRoomRepository;
import taxi.tago.repository.TaxiUserRepository;
import taxi.tago.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 채팅 메시지 저장 / 조회 비즈니스 로직ㅇ르 담당하는 서비스 계층
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용 트랜잭션
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository; // 메시지 테이블 접근
    private final ChatRoomRepository chatRoomRepository; // 채팅방 조회
    private final UserRepository userRepository; // 보낸 유저 조회
    private final TaxiUserRepository taxiUserRepository; // 동승슈니 정보 조회

    // 텍스트 채팅 메시지 전송(저장) 메서드
    // request: 클라이언트에서 넘어온 전송 요청 DTO
    // senderId: 현재 로그인(= WebSocket 인증)된 사용자 ID
    // 반환값: 저장된 메시지를 기반으로 만든 응답 DTO
    @Transactional // DB write가 발생하므로 readOnly = false
    public ChatMessageResponse sendTextMessage(ChatMessageSendRequest request, Long senderId) {
        // 채팅방 존재 여부 검증
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 채팅방이 존재하지 않습니다. chatRoomId = " + request.getChatRoomId()
                ));

        // 채팅방이 이미 종료된 방인지 검증
        if (chatRoom.isClosed()) {
            throw new IllegalArgumentException("이미 종료된 채팅방입니다. 새로운 택시팟을 생성해주세요.");
        }

        // 보낸 유저 엔티티 조회
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 유저가 존재하지 않습니다. userId = " + senderId
                ));

        // 해당 유저가 이 택시팟 채팅방에 자격이 있는지 검증 (총대슈니 or ACCEPTED 동승슈니)
        validateChatMember(chatRoom.getTaxiParty(), senderId);

        // ChatMessage 엔티티 생성 (정적 팩토리 메서드 사용)
        ChatMessage message = ChatMessage.createTextMessage(
                chatRoom,
                sender,
                request.getContent()
        );

        // 채팅방의 최근 메시지, 시간 정보 업데이트
        // (sentAt은 @CreationTimestamp로 DB insert 시점에 찍히므로 여기서는 "지금 시각"을 사용해서 ChatRoom 요약 정보를 갱신)
        LocalDateTime now = LocalDateTime.now();
        chatRoom.updateMessage(request.getContent(), now);

        // 메시지 엔티티를 DB에 INSERT
        ChatMessage saved = chatMessageRepository.save(message);

        // 엔티티를 클라이언트 응답에 사용할 DTO로 변환
        return ChatMessageResponse.from(saved);
    }

    // 특정 채팅방의 전체 메시지(또는 최근 메시지들)를 시간순으로 조회 (채팅방 입장 시 이전 대화 내용 불러오는 용도)
    // chatRoomId: 채팅방 ID
    // userId: 현재 로그인한 사용자 ID (권한 검증용)
    // 반환값: 시간순 정렬된 메시지 응답 DTO 리스트
    public List<ChatMessageResponse> getMessages(Long chatRoomId, Long userId) {
        // 채팅방 존재 여부 검증
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 채팅방이 존재하지 않습니다. chatRoomId = " + chatRoomId
                ));

        // 조회하려는 유저가 이 채팅방의 멤버인지 검증
        validateChatMember(chatRoom.getTaxiParty(), userId);

        // 해당 채팅방의 메시지 전체를 sentAt 오름차순으로 조회
        List<ChatMessage> messages =
                chatMessageRepository.findByChatRoom_IdOrderBySentAtAsc(chatRoomId);

        // 엔티티 리스트 -> DTO 리스트 변환
        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    // 유저가 택시팟 채팅방에 참여할 자격이 있는지 검증하는 내부 메서드 (총대슈니 or ACCEPTED 동승슈니만 채팅 가능)
    private void validateChatMember(TaxiParty taxiParty, Long userId) {
        // 택시팟의 총대인지 확인
        boolean isHost = taxiParty.getUser().getId().equals(userId);

        // 동승슈니로 참여했는지 조회 (WAITING / ACCEPTED 등)
        Optional<TaxiUser> taxiUserOpt = taxiUserRepository.findByTaxiPartyIdAndUserId(taxiParty.getId(), userId);

        // 그 중에서도 상태가 ACCEPTED인지 확인
        boolean isAcceptedPassenger = taxiUserOpt
                .filter(tu -> tu.getStatus() == ParticipationStatus.ACCEPTED)
                .isPresent();

        // 둘 다 아니라면 채팅 권한 없음
        if (!isHost && !isAcceptedPassenger) {
            throw new IllegalArgumentException(
                    "채팅 권한이 없습니다. 같이 타기 요청이 수락된 이후에만 채팅이 가능합니다."
            );
        }
    }
}
