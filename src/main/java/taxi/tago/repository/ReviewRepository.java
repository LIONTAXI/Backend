package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import taxi.tago.entity.Review;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 이미 특정 택시팟(taxiPartyId)에서 작성자(reviewId)가 같은 대상자(reviewerId)에게 리뷰를 작성한 적이 있는지 여부 확인 (중복 작성 방지)
    boolean existsByTaxiPartyIdAndReviewerIdAndRevieweeId(Long taxiPartyId, Long reviewerId, Long revieweeId);

    // 특정 사용자가 받은 모든 리뷰 조회
    List<Review> findByRevieweeId(Long userId);

    // 특정 사용자가 받은 리뷰 중 "다시 만나고 싶어요"인 리뷰 수 (want_to_meet_again = true인 것만 카운트)
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :userId AND r.wantToMeetAgain = true")
    Long countPositiveMatchPreference(Long userId);

    // 특정 사용자가 받은 전체 리뷰 수
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :userId")
    Long countTotalReviews(Long userId);

    // 특정 대상자가 받은 긍정 태그 개수 집계
    @Query(value =
            "SELECT tag, COUNT(*) AS cnt " +
                    "FROM review_positive_tags " +
                    "WHERE review_id IN (SELECT review_id FROM reviews WHERE reviewee_id = :userId) " +
                    "GROUP BY tag",
            nativeQuery = true
    )
    List<Object[]> countPositiveTags(Long userId);

    // 특정 대상자가 받은 부정 태그 개수 집계
    @Query(value =
            "SELECT tag, COUNT(*) AS cnt " +
                    "FROM review_negative_tags " +
                    "WHERE review_id IN (SELECT review_id FROM reviews WHERE reviewee_id = :userId) " +
                    "GROUP BY tag",
            nativeQuery = true
    )
    List<Object[]> countNegativeTags(Long userId);
}
