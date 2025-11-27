package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.entity.TaxiParty;

import java.util.List;

public interface TaxiPartyRepository extends JpaRepository<TaxiParty, Long> {

    // 현재 '매칭 중'인 글을 리스트로 가져옴
    List<TaxiParty> findAllByStatusOrderByCreatedAtDesc(TaxiPartyStatus status);

    // 현재 '매칭 중'인 글에서 사용 중인 이모지 가져오는 쿼리
    @Query("SELECT t.markerEmoji FROM TaxiParty t WHERE t.status = :status")
    List<String> findAllEmojisByStatus(@Param("status") TaxiPartyStatus status);
}