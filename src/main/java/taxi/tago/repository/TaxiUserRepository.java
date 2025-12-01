package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.entity.TaxiUser;

import java.util.List;
import java.util.Optional;

public interface TaxiUserRepository extends JpaRepository<TaxiUser, Long> {
    Boolean existsByTaxiPartyIdAndUserId(Long taxiPartyId, Long userId);
    List<TaxiUser> findAllByTaxiPartyId(Long taxiPartyId);
    Optional<TaxiUser> findByTaxiPartyIdAndUserId(Long taxiPartyId, Long userId);

    // 내가 ACCEPTED 상태로 들어간 택시팟들
    List<TaxiUser> findAllByUserIdAndStatus(Long userId, ParticipationStatus status);
}