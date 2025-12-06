package taxi.tago.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

// WebSocket/STOMP로 들어오는 메시지 전송 요청 DTO (클라이언트 → 서버 방향)
@Getter
@NoArgsConstructor
public class ChatMessageSendRequest {

    // 어떤 채팅방으로 보낼 메시지인지
    private Long chatRoomId;

    // 실제 채팅 내용
    private String content;
}
