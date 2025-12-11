package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 채팅 메시지 엔티티
@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatmessage_id")
    private Long id;

    // 메시지가 속한 채팅방 (N:1)
    @ManyToOne(fetch = FetchType.LAZY) // 지연로딩
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoom chatRoom;

    // 메시지를 보낸 사용자 정보 (N:1)
    // 프로필 사진, 닉네임 등을 조회할 수 있도록 User와 매핑
    @ManyToOne(fetch = FetchType.LAZY) // 지연로딩
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 메시지 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    // 실제 채팅 내용
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    // 메시지 전송 시각
    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // 일반 텍스트 메시지용 팩토리 메서드
    public static ChatMessage createTextMessage(ChatRoom chatRoom, User sender, String content) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("메시지 생성 시 ChatRoom은 필수입니다.");
        }
        if (sender == null) {
            throw new IllegalArgumentException("메시지 생성 시 sender는 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }

        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.messageType = MessageType.TEXT; // 일반 채팅
        message.content = content;
        return message;
    }

    // 시스템 안내 메시지용 팩토리 메서드
    public static ChatMessage createSystemMessage(ChatRoom chatRoom, User sender, String content) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("메시지 생성 시 ChatRoom은 필수입니다.");
        }
        if (sender == null) {
            throw new IllegalArgumentException("메시지 생성 시 sender는 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }

        ChatMessage message = new ChatMessage();
        message.chatRoom = chatRoom;
        message.sender = sender;
        message.messageType = MessageType.SYSTEM; // 시스템 메시지로 구분
        message.content = content;
        return message;
    }

    // ENUM
    // TEXT: 일반 채팅 메시지
    // SYSTEM: 시스템 안내 메시지
    public enum MessageType {
        TEXT,
        SYSTEM
    }
}
