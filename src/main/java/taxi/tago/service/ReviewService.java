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
    private final SettlementParticipantRepository settlementParticipantRepository; // 미정산 이력 개수를 계산하기 위해 사용

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

    // 채팅창 메뉴 페이지 하단에서 "사용자 목록" 페이지 화면에 보여줄 데이터 조회 메서드
    @Transactional(readOnly = true)
    public List<ReviewDto.MemberReviewStatus> getMemberReviewStatusList(Long taxiPartyId, Long currentUserId) {
        // 택시팟 조회
        TaxiParty taxiParty = taxiPartyRepository.findById(taxiPartyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 택시팟을 찾을 수 없습니다. id = " + taxiPartyId
                ));
        // 현재 유저가 이 택시팟 멤버인지 먼저 검증
        validateMemberOfTaxiParty(taxiParty, currentUserId);

        // 택시팟의 총대 + ACCEPTED 동승 멤버 모두 조회
        List<User> members = getAllMembersOfTaxiParty(taxiParty);

        // 각 멤버에 대해 현재 유저가 이미 리뷰를 작성했는지 여부를 계산
        List<ReviewDto.MemberReviewStatus> result = new ArrayList<>();

        for (User member : members) {
            boolean isHost = taxiParty.getUser().getId().equals(member.getId());

            // 자기 자신에겐 reviewWritten = false로 내려주기
            boolean reviewWritten = false;

            if (!member.getId().equals(currentUserId)) {
                // 현재 유저가 이 멤버에게 이미 리뷰를 작성했는지 확인
                reviewWritten = reviewRepository.existsByTaxiPartyIdAndReviewerIdAndRevieweeId(
                        taxiPartyId,
                        currentUserId,
                        member.getId()
                );
            }

            ReviewDto.MemberReviewStatus dto = new ReviewDto.MemberReviewStatus(
                    member.getId(),
                    member.getName(),
                    member.getShortStudentId(),
                    member.getImgUrl(),
                    isHost,
                    reviewWritten
            );
            result.add(dto);
        }

        return result;
    }

    // 프로필 화면에서 사용하는 요약 정보 조회 메서드
    @Transactional(readOnly = true)
    public ReviewDto.ProfileSummaryResponse getProfileSummary(Long targetUserId) {
        // 프로필 대상 유저 기본 정보 조회
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다. id = " + targetUserId
                ));

        // 재매칭 희망률 계산
        Long totalReviews = reviewRepository.countTotalReviews(targetUserId);
        Long positiveMatchCount = reviewRepository.countPositiveMatchPreference(targetUserId);

        Integer matchPreferenceRate = null;
        if (totalReviews != null && totalReviews > 0) {
            // (다시 만나고 싶어요 개수 / 전체 리뷰 수) * 100
            double rate = (positiveMatchCount.doubleValue() / totalReviews.doubleValue()) * 100.0;
            // 소수점 반올림해서 정수로 변환
            matchPreferenceRate = (int) Math.round(rate);
        }

        // 미정산 이력 개수 계산
        Long unpaidCountLong = settlementParticipantRepository.countByUserIdAndPaidFalse(targetUserId);
        Integer unpaidCount = unpaidCountLong != null ? unpaidCountLong.intValue() : 0;

        // 긍정 태그 / 부정 태그 카운트 조회
        List<ReviewDto.TagCount> positiveTagCounts =
                toTagCountList(reviewRepository.countPositiveTags(targetUserId));
        List<ReviewDto.TagCount> negativeTagCounts =
                toTagCountList(reviewRepository.countNegativeTags(targetUserId));

        // 최종 DTO 생성 및 반환
        return new ReviewDto.ProfileSummaryResponse(
                target.getId(),
                target.getName(),
                target.getShortStudentId(),
                target.getImgUrl(),
                matchPreferenceRate,
                unpaidCount,
                positiveTagCounts,
                negativeTagCounts
        );
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
