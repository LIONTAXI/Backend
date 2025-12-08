package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.Settlement;
import taxi.tago.entity.TaxiParty;

import java.util.Optional;

// Settlement 엔티티용 JPA Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 특정 택시팟에 대한 정산 내역 조회
    Optional<Settlement> findByTaxiParty(TaxiParty taxiParty);
}
