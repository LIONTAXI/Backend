package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.entity.ChatRoom;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.TaxiUser;
import taxi.tago.repository.ChatRoomRepository;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.TaxiUserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용 트랜잭션(SELECT) -> DB 쓰기가 필요한 메서드만 오버라이드로 명시
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository; // 채팅방 엔티티에 접근하기 위한 의존성
    private final TaxiPartyRepository taxiPartyRepository; // 택시팟 정보를 조회하기 위한 의존성
    private final TaxiUserRepository taxiUserRepository; // 택시팟 참졍자 정보를 조회하기 위한 의존성

    // 채팅방 입장/생성 메서드 (DB쓰기가 필요하므로 트랜잭션 별도 지정)
    // 택시팟 ID와 유저 ID로 채팅방에 입장하거나 새로 생성함
    @Transactional
    public ChatRoom enterOrCreateChatRoom(Long taxiPartyId, Long userId) {
        // 택시팟 존재 여부 먼저 검증
        TaxiParty taxiParty = taxiPartyRepository.findById(taxiPartyId) // ID로 택시팟 조회
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다. id = " + taxiPartyId)); // 400

        // 현재 유저가 이 택시팟의 채팅방에 입장할 자격이 있는지 검증
        validateChatMember(taxiParty, userId);

        // 이미 이 택시팟에 연결된 채팅방이 있는지 조회
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findByTaxiPartyId(taxiPartyId); // taxiPartyId 기준 조회

        // 채팅방이 이미 존재한다면 그대로 반환
        if (existingRoomOpt.isPresent()) {
            ChatRoom room = existingRoomOpt.get(); // 실제 ChatRoom 엔티티 꺼내옴

            if (room.isClosed()) {
                throw new IllegalArgumentException("종료된 채팅방입니다. 새로운 택시팟을 만들어주세요.");
            }

            return room; // 문제가 없다면 기존 채팅방 엔티티 반환
        }

        // 채팅방이 아지기 없다면 새로 생성
        ChatRoom newRoom = ChatRoom.create(taxiParty); // TaxiParty를 인자로 받는 팩토리 메서드를 사용해 ChatRoom 엔티티 생성

        // 생성한 채팅방을 DB에 저장
        ChatRoom saved = chatRoomRepository.save(newRoom); // JPA를 통해 insert 쿼리를 날리고 영속 상태 엔티티를 반환

        // 저장된 채팅방 엔티티 반환 (ID 등 PK 값이 채워진 상태)
        return saved; // 컨트롤러나 WebSocket 핸들러에서 이 엔티티를 기반으로 응답에 사용
    }

    // 특정 택시팟 + 유저조합이 채팅방 입장 자격이 있는지 검증하는 내부 메서드 (권한 검증용 메서드)
    private void validateChatMember(TaxiParty taxiParty, Long userId) {
        // 택시팟의 총대슈니인지 먼저 확인
        boolean isHost = taxiParty.getUser().getId().equals(userId); // TaxiParty에 연결된 User의 ID와 비교

        // 동승슈니로 참여했는지 조회 (WAITING/ACCEPTED/기타 상태)
        Optional<TaxiUser> taxiUserOpt = taxiUserRepository.findByTaxiPartyIdAndUserId(taxiParty.getId(), userId);

        // ACCEPTED 상태의 동승슈니인지 여부 계산
        boolean isAcceptedPassenger = taxiUserOpt // 조회 결과 Optional에서
                .filter(taxiUser -> taxiUser.getStatus() == ParticipationStatus.ACCEPTED) // 상태가 ACCEPTED인 경우만 통과
                .isPresent(); // 최종적으로 존재 여부를 boolean으로 반환

        // 총대도 아니고 ACCEPTED 동승슈니도 아니라면 입장 권한 없음
        if (!isHost && !isAcceptedPassenger) {
            throw new IllegalArgumentException("채팅방 입장 권한이 없습니다. 같이 타기 요청이 수락된 후에만 채팅방에 입장할 수 있습니다."); // 400
        }
    }
}
