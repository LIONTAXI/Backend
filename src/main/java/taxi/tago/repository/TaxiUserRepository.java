package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.entity.TaxiUser;

import java.util.List;

public interface TaxiUserRepository extends JpaRepository<TaxiUser, Long> {
    Boolean existsByTaxiPartyIdAndUserId(Long taxiPartyId, Long userId);
    List<TaxiUser> findAllByTaxiPartyId(Long taxiPartyId);
}