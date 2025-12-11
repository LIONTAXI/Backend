package taxi.tago.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import taxi.tago.entity.ChatMessage;

import java.time.LocalDateTime;

// WebSocket/STOMP로 브로드캐스트할 응답 DTO (서버 → 구독 중인 클라이언트들 방향)
@Getter
@NoArgsConstructor
public class ChatMessageResponse {

    private Long messageId;
    private Long chatRoomId;
    private Long senderId;
    private String name;          // 이름
    private String shortStudentId; // 학번 (2자리, 예: "23")

    private String content;

    // 메시지 타입(TEXT / SYSTEM)을 구분할 수 있도록 추가
    private String messageType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    public ChatMessageResponse(
            Long messageId,
            Long chatRoomId,
            Long senderId,
            String name,
            String shortStudentId,
            String content,
            String messageType,
            LocalDateTime sentAt
    ) {
        this.messageId = messageId;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.name = name;
        this.shortStudentId = shortStudentId;
        this.content = content;
        this.messageType = messageType;
        this.sentAt = sentAt;
    }

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getSender().getShortStudentId(),
                message.getContent(),
                message.getMessageType().name(), // ENUM → "TEXT" / "SYSTEM" 으로 내려줌
                message.getSentAt()
        );
    }
}
