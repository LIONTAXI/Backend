package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.dto.ReviewDto;
import taxi.tago.entity.Review;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.TaxiUser;
import taxi.tago.entity.User;
import taxi.tago.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 택시팟 후기 관련 비즈니스 로직을 담당하는 서비스 클래스
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final TaxiPartyRepository taxiPartyRepository; // 택시팟 정보 조회용
    private final UserRepository userRepository; // 유저 정보 조회용
    private final TaxiUserRepository taxiUserRepository; // 동승슈니 매핑 조회용
    private final ReviewRepository reviewRepository; // 후기를 실제로 저장/조회하는 레포지토리
    private SettlementParticipantRepository settlementParticipantRepository; // 미정산 이력 개수를 계산하기 위해 사용

    // 후기 작성 메서드
    @Transactional
    public Long createReview(Long reviewerId, ReviewDto.CreateRequest request) {
        // 택시팟 조회
        TaxiParty taxiParty = taxiPartyRepository.findById(request.getTaxiPartyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 택시팟을 찾을 수 없습니다. id = " + request.getTaxiPartyId()
                ));

        // 작성자 / 대상자 유저 조회
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "리뷰 작성자를 찾을 수 없습니다. id = " + reviewerId
                ));

        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "리뷰 대상자를 찾을 수 없습니다. id = " + request.getRevieweeId()
                ));

        // 자기 자신에게 후기 작성 방지
        if (reviewerId.equals(request.getRevieweeId())) {
            throw new IllegalArgumentException("본인에게 후기를 남길 수 없습니다.");
        }

        // 이 택시팟의 멤버인지 검증
        validateMemberOfTaxiParty(taxiParty, reviewerId);
        validateMemberOfTaxiParty(taxiParty, request.getRevieweeId());

        // 이미 이 조합(taxiParty, reviewer, reviewee)으로 리뷰가 존재하는지 확인
        boolean alreadyExists = reviewRepository
                .existsByTaxiPartyIdAndReviewerIdAndRevieweeId(
                        taxiParty.getId(),
                        reviewerId,
                        request.getRevieweeId()
                );
        if (alreadyExists) {
            throw new IllegalArgumentException("이미 이 택시팟에서 해당 슈니에게 후기를 작성했습니다.");
        }

        // 작성자가 총대인지 여부 계산 (UI에서 총대슈니/동승슈니 구분용)
        boolean reviewerIsHost = taxiParty.getUser().getId().equals(reviewerId);

        // Review 인스턴스 생성
        Review review = Review.create(
                taxiParty,
                reviewer,
                reviewee,
                reviewerIsHost,
                request.isWantToMeetAgain(),
                request.getPositiveTags(),
                request.getNegativeTags()
        );

        // DB 저장
        Review saved = reviewRepository.save(review);

        log.info("리뷰 생성 완료: reviewId={}, taxiPartyId={}, reviewerId={}, revieweeId={}",
                saved.getId(), taxiParty.getId(), reviewerId, reviewee.getId());

        return saved.getId();
    }


    /* 내부 유틸 메서드 */

    // 하나의 택시팟에 속한 모든 멤버를 조회하는 메서드
    private List<User> getAllMembersOfTaxiParty(TaxiParty taxiParty) {
        // 총대 (host) 추가
        List<User> members = new ArrayList<>();
        members.add(taxiParty.getUser());

        // taxi_user에서 이 팟에 속한 동승슈니 목록 조회
        List<TaxiUser> passengers = taxiUserRepository.findAllByTaxiPartyId(taxiParty.getId());

        // 그 중에서 status == ACCEPTED인 사람만 추가
        members.addAll(
                passengers.stream()
                        .filter(tu -> tu.getStatus() == ParticipationStatus.ACCEPTED)
                        .map(TaxiUser::getUser)
                        .toList()
        );

        return members;
    }

    // 주어진 userId가 해당 택시팟의 멤버인지 검증하는 메서드
    private void validateMemberOfTaxiParty(TaxiParty taxiParty, Long userId) {
        // 총대인지 먼저 확인
        if (taxiParty.getUser().getId().equals(userId)) {
            return;
        }

        // 동승슈니로 ACCEPTED 상태인지 확인
        Optional<TaxiUser> taxiUserOpt =
                taxiUserRepository.findByTaxiPartyIdAndUserId(taxiParty.getId(), userId);

        boolean acceptedPassenger = taxiUserOpt
                .filter(tu -> tu.getStatus() == ParticipationStatus.ACCEPTED)
                .isPresent();

        if (!acceptedPassenger) {
            throw new IllegalArgumentException("해당 택시팟의 멤버만 후기 작성/조회가 가능합니다.");
        }
    }

    // nativeQuery 결과(List<Object[]>)를 ReviewDto.TagCount 리스트로 변환하는 메서드
    private List<ReviewDto.TagCount> toTagCountList(List<Object[]> rows) {
        // 리스트가 비어있는지 확인
        if (rows == null || rows.isEmpty()) {
            return List.of(); // 빈 불변 리스트 반환
        }

        return rows.stream()
                .map(row -> {
                    String tag = (String) row[0]; // 첫 번째 컬럼: 태그명
                    Long count = ((Number) row[1]).longValue(); // 두 번째 컬럼: 개수
                    return new ReviewDto.TagCount(tag, count); // DTO 생성
                }).toList();
    }
}
