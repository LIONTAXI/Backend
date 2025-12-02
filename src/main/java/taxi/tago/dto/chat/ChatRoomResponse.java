package taxi.tago.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import taxi.tago.entity.ChatRoom;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor // json 역직렬화
@AllArgsConstructor
public class ChatRoomResponse {

    // 채팅방 고유 ID (웹소켓 구독 경로, 채팅방 상세 페이지 진입 시 사용)
    private Long chatRoomId;

    // 이 채팅방이 연결된 택시팟 ID (채팅방에서 어떤 택시팟인지를 알기 위해)
    private Long taxiPartyId;

    // 채팅방 종료 여부
    private boolean closed;

    // 채팅방 생성 시각
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // 채팅 목록에서 보여줄 마지막 메시지 내용
    private String lastMessage;

    // 마지막 메시지가 전송된 시각
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt;

    // 엔티티 -> DTO 변환을 위한 정적 팩토리 메서드
    public static ChatRoomResponse from(ChatRoom room) {
        if (room == null) {
            return null;
        }

        return new ChatRoomResponse(
                room.getId(),
                room.getTaxiParty().getId(),
                room.isClosed(),
                room.getCreatedAt(),
                room.getLastMessage(),
                room.getLastMessageAt()
        );
    }
}
