package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.SettlementParticipant;

import java.util.List;
import java.util.Optional;

// SettlementParticipant 엔티티용 JPA Repository
public interface SettlementParticipantRepository extends JpaRepository<SettlementParticipant, Long> {

    // 특정 정산에 속한 모든 참여자 조회
    List<SettlementParticipant> findBySettlementId(Long settlementId);

    // 특정 정산에서 특정 유저의 정산 정보 조회
    Optional<SettlementParticipant> findBySettlementIdAndUserId(Long settlementId, Long userId);

    Long countByUserIdAndPaidFalse(Long userId);
}
