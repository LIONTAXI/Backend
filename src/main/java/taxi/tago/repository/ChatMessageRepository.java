package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.ChatMessage;

import java.util.List;

// ChatMessage 엔티티용 JPA Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 메시지를 sentAt 기준 오름차순으로 조회하는 메서드
    List<ChatMessage> findByChatRoom_IdOrderBySentAtAsc(Long chatRoomId);
}
