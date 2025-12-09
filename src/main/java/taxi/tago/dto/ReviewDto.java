package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import taxi.tago.entity.Review;

import java.util.List;
import java.util.Set;

// 택시팟 후기 관련 DTO 묶음 클래스
public class ReviewDto {

    // 후기 작성 요청 DTO
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        // 어떤 택시팟에 대한 후기인지
        private Long taxiPartyId;

        // 후기 대상자(상대방) ID
        private Long revieweeId;

        // 선택한 "매너 평가" 태그 목록
        private Set<Review.PositiveReviewTag> positiveTags;

        // 선택한 "비매너 평가" 태그 목록
        private Set<Review.NegativeReviewTag> negativeTags;

        // 재매칭 희망 여부
        private boolean wantToMeetAgain;
    }

    // 채팅방 사용자 목록 화면용 DTO
    @Getter
    @AllArgsConstructor
    public static class MemberReviewStatus {
        // 후기 대상자 userId
        private Long userId;

        // 이름
        private String name;

        // 학번
        private String shortStudentId;

        // 프로필 이미지 URL
        private String imgUrl;

        // 이 멤버가 이 택시팟의 총대인지 여부
        private boolean host;

        // 현재 로그인 유저가 이 멤버에게 이미 후기를 작성했는지 여부
        private boolean reviewWritten;
    }

    // 프로필 화면 요약 정보 DTO
    @Getter
    @AllArgsConstructor
    public static class ProfileSummaryResponse {
        // 프로필 대상자 정보
        private Long userId;
        private String name;
        private String shortStudentId;
        private String imgUrl;

        // 재매칭 희망률 (0 - 100%)
        // - (다시 만나고 싶어요 리뷰 수 / 전체 리뷰 수) * 100
        // - 리뷰가 0건인 경우엔 null
        private Integer matchPreferenceRate;

        // 미정산 이력 개수
        private Integer unpaidCount;

        // 받은 매너 평가 (긍정 태그) 카운트 목록
        private List<TagCount> positiveTagCounts;

        // 받은 비매너 평가 (부정 태그) 카운트 목록
        private List<TagCount> negativeTagCounts;
    }

    // 태그별 카운트 구성을 위한 재사용 DTO
    @Getter
    @AllArgsConstructor
    public static class TagCount {
        // enum name
        private String tag;

        // 이 태그를 받은 횟수
        private Long count;
    }

    // 단일 후기 상세 응답 DTO
    @Getter
    @AllArgsConstructor
    public static class SingleReviewDetailResponse {
        // 어떤 리뷰인지 식별용
        private Long reviewId;

        // 이 후기가 어떤 택시팟에서 작성된 것인지
        private Long taxiPartyId;

        // 리뷰 작성자(상대방) 기본 정보
        private Long reviewerId;
        private String reviewerName;
        private String reviewerShortStudentId;
        private String reviewerImgUrl;

        // 리뷰 작성자의 전체 히스토리 기반 요약 정보
        private Integer matchPreferenceRate;
        private Integer unpaidCount;

        // 이 리뷰 한 건에서 받은 긍정 / 부정 태그 목록
        private List<String> positiveTags;
        private List<String> negativeTags;

        // 알림을 누른 유저(= reviewee)가 아직 상대방에게 후기를 쓰지 않았다면 true
        // (true: "나도 후기 작성하러 가기" 버튼 노출 / false: 이미 썼으므로 "확인" 버튼 노출)
        private boolean canWriteBack;
    }
}
