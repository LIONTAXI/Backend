package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 택시팟 그룹 채팅방 엔티티 클래스
@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id // 기본키 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    @Column(name = "chatroom_id")
    private Long id; // 채팅방 고유 ID

    @OneToOne(fetch = FetchType.LAZY) // TaxiParty와 1:1 관계
    @JoinColumn(name = "taxiparty_id", nullable = false, unique = true) // 외래키, 하나의 택시팟에 채팅방 하나만 생성되도록 unique 설정
    private TaxiParty taxiParty; // 이 채팅방이 속한 택시팟

    @CreationTimestamp // 엔티티 최초 저장 시 자동 시간 기록
    @Column(name = "created_at", nullable = false, updatable = false) // 생성 시각은 수정 불가
    private LocalDateTime createdAt; // 채팅방 생성 시각

    @Column(name = "closed_at") // 종료 시각 컬럼
    private LocalDateTime closedAt; // 채팅방 종료 시각

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false; // 채팅방 종료 여부 플래그

    @Column(name = "last_message", length = 255) // 최근 메시지 컬럼
    private String lastMessage; // 채팅 목록 화면에 보여줄 마지막 메시지 내용

    @Column(name = "last_message_at") // 최근 메시지 시간 컬럼
    private LocalDateTime lastMessageAt; // 마지막 메시지 전송 시각

    // 팩토리 메서드
    // - 외부에서 생성 시 규칙을 강제하기 위해 사용
    // - 항상 TaxiParty가 있어야만 채팅방을 만들 수 있도록 제한
    public static ChatRoom create(TaxiParty taxiParty) {
        if (taxiParty == null) {
            throw new IllegalArgumentException("채팅방 생성 시 TaxiParty는 필수입니다.");
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.taxiParty = taxiParty; // 어떤 택시팟의 채팅방인지 연결
        chatRoom.closed = false; // 기본값: 아직 종료되지 않음
        return chatRoom;
    }

    // 채팅방 종료 비즈니스 로직
    public void close() {
        if (!this.closed) { // 이미 종료된 방일 경우 중복 처리 방지
            this.closed = true;
            this.closedAt = LocalDateTime.now(); // 종료 시각 기록
        }
    }

    // 최근 메시지 정보 갱신 로직
    // - 새 메시지가 저장될 때마다 ChatMessage 쪽에서 호출 예정
    public void updateMessage(String messageContent, LocalDateTime sentAt) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return; // 비어있는 메시지는 무시
        }

        this.lastMessage = messageContent; // 최근 메시지 내용 업데이트
        this.lastMessageAt = sentAt != null // 보낸 시간이 null이라면
                ? sentAt // 전달받은 시간 사용
                : LocalDateTime.now(); // 아니면 현재 시간으로 대체
    }
}
