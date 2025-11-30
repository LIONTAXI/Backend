package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.ChatRoom;

import java.util.Optional;

// ChatRoom 엔티티에 대한 JPA 레포지토리 인터페이스 선언
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 택시팟 ID로 채팅방을 조회하는 메서드 시그니처
    Optional<ChatRoom> findByTaxiPartyId(Long taxiPartyId);
}
