package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.TaxiUser;

public interface TaxiUserRepository extends JpaRepository<TaxiUser, Long> {
    Boolean existsByTaxiPartyIdAndUserId(Long taxiPartyId, Long userId);
}