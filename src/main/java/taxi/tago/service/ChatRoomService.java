package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.dto.chat.ChatRoomSummaryResponse;
import taxi.tago.dto.chat.MyChatRoomListResponse;
import taxi.tago.entity.ChatRoom;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.TaxiUser;
import taxi.tago.repository.ChatRoomRepository;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.TaxiUserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용 트랜잭션(SELECT) -> DB 쓰기가 필요한 메서드만 오버라이드로 명시
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository; // 채팅방 엔티티에 접근하기 위한 의존성
    private final TaxiPartyRepository taxiPartyRepository; // 택시팟 정보를 조회하기 위한 의존성
    private final TaxiUserRepository taxiUserRepository; // 택시팟 참졍자 정보를 조회하기 위한 의존성

    // 공통 채팅 권한 검증 서비스
    private final ChatMemberAccessService chatMemberAccessService;

    // 채팅방 입장/생성 메서드
    // 택시팟 ID와 유저 ID로 채팅방에 입장하거나 새로 생성함
    @Transactional // DB write가 필요하므로 트랜잭션 별도 지정
    public ChatRoom enterOrCreateChatRoom(Long taxiPartyId, Long userId) {
        // 택시팟 존재 여부 먼저 검증
        TaxiParty taxiParty = taxiPartyRepository.findById(taxiPartyId) // ID로 택시팟 조회
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다. id = " + taxiPartyId)); // 400

        // 공통 서비스로 채팅 입장 자격 검증
        boolean hasPermission = chatMemberAccessService.hasChatPermission(taxiParty, userId);
        if (!hasPermission) {
            // 기존 메시지 그대로 유지
            throw new IllegalArgumentException("채팅방 입장 권한이 없습니다. 같이 타기 요청이 수락된 후에만 채팅방에 입장할 수 있습니다.");
        }

        // 이미 이 택시팟에 연결된 채팅방이 있는지 조회
        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findByTaxiPartyId(taxiPartyId); // taxiPartyId 기준 조회

        // 채팅방이 이미 존재한다면 그대로 반환
        if (existingRoomOpt.isPresent()) {
            ChatRoom room = existingRoomOpt.get(); // 실제 ChatRoom 엔티티 꺼내옴
            // 이미 종료된 채팅방이면 입장 불가
            if (room.isClosed()) {
                throw new IllegalArgumentException("종료된 채팅방입니다. 새로운 택시팟을 만들어주세요.");
            }

            return room; // 문제가 없다면 기존 채팅방 엔티티 반환
        }

        // 채팅방이 아직 없다면 새로 생성
        ChatRoom newRoom = ChatRoom.create(taxiParty); // TaxiParty를 인자로 받는 팩토리 메서드를 사용해 ChatRoom 엔티티 생성

        // 생성한 채팅방을 DB에 INSERT
        ChatRoom saved = chatRoomRepository.save(newRoom); // JPA를 통해 insert 쿼리를 날리고 영속 상태 엔티티를 반환

        // PK가 채워진 영속 상태의 엔티티 반환
        return saved; // 컨트롤러나 WebSocket 핸들러에서 이 엔티티를 기반으로 응답에 사용
    }

    // 내 채팅방 목록을 조회하는 메서드
    public MyChatRoomListResponse getMyChatRooms(Long userId) {
        // 내가 총대인 채팅방들 조회
        List<ChatRoom> hostRooms = chatRoomRepository.findByTaxiParty_User_Id(userId);

        // 내가 ACCEPTED 동승슈니인 TaxiUser 엔티티들 조회
        List<TaxiUser> acceptedList = taxiUserRepository.findAllByUserIdAndStatus(userId, ParticipationStatus.ACCEPTED);

        // 위 TaxiUser 목록에서 택시팟 ID만 뽑아서 중복 제거
        List<Long> taxiPartyIds = acceptedList.stream()
                .map(tu -> tu.getTaxiParty().getId())
                .distinct()
                .toList();

        // 해당 택시팟 ID들에 연결된 채팅방 조회
        List<ChatRoom> passengerRooms = taxiPartyIds.isEmpty() ? List.of() : chatRoomRepository.findByTaxiParty_IdIn(taxiPartyIds);

        // 총대 + 동승슈니 채팅방을 하나의 스트림으로 합친 뒤, distinct()로 중복 제거
        List<ChatRoom> allRooms = Stream.concat(hostRooms.stream(), passengerRooms.stream())
                .distinct().toList();

        // 응답용 DTO 리스트 두 개 준비
        List<ChatRoomSummaryResponse> matchingRooms = new ArrayList<>();
        List<ChatRoomSummaryResponse> finishedRooms = new ArrayList<>();

        // closed 여부, TaxiPartyStatus로 분류
        for (ChatRoom room : allRooms) {
            // "택시팟 끝내기" 눌러서 닫힌 방은 목록에서 제외
            if (room.isClosed()) {
                continue;
            }

            ChatRoomSummaryResponse dto = ChatRoomSummaryResponse.from(room);

            // TaxiParty의 상태에 따라 리스트 분기
            switch (room.getTaxiParty().getStatus()) {
                case MATCHING -> matchingRooms.add(dto); // 지금 매칭중인 택시팟
                case FINISHED -> finishedRooms.add(dto); // 지난 택시팟
            }
        }

        // 두 리스트를 하나의 wrapper DTO로 감싸서 반환
        return new MyChatRoomListResponse(matchingRooms, finishedRooms);
    }

    // 택시팟 끝내기(채팅방 종료) 메서드
    @Transactional // DB에 UPDATE가 발생하므로 write 트랜잭션으로 설정
    public void closeChatRoom(Long chatRoomId, Long userId) {
        // 채팅방 엔티티 조회
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방이 존재하지 않습니다. id = " + chatRoomId));

        // 채팅방이 속한 택시팟 엔티티 꺼내기
        TaxiParty party = room.getTaxiParty();

        // 현재 요청자가 이 택시팟의 총대슈니인지 확인
        // party.user.id와 요청 userId가 같아야만 종료 권한이 있음
        if (!party.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("총대슈니만 택시팟을 끝낼 수 있습니다.");
        }

        // 이미 closed = true인 방이라면 중복 요청이므로 예외 처리
        if (room.isClosed()) {
            throw new IllegalArgumentException("이미 종료된 채팅방입니다.");
        }

        // 엔티티 내부 비즈니스 메서드 호출 (closed=true, closedAt=now로 상태 변경)
        room.close();

        // @Transactional 때문에 메서드가 정상 종료되면 변경사항이 자동으로 flush되어 UPDATE 쿼리가 나가므로, save() 호출할 필요가 없음
    }
}
