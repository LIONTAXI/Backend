package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.ReviewDto;
import taxi.tago.security.CustomUserDetails;
import taxi.tago.service.ReviewService;

import java.util.List;

// 택시팟 후기 관련 REST API 컨트롤러
// POST   /api/reviews : 후기 작성
// GET    /api/reviews/members : 채팅방 → 사용자 목록 화면 데이터 조회
// GET    /api/reviews/profile/{id} : 사용자 프로필 요약 정보 조회
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(
        name = "후기 API",
        description = "택시팟 후기를 작성하고, 사용자 목록/프로필에서 후기 정보를 조회하는 API"
)
public class ReviewController {

    private final ReviewService reviewService;

    // 후기 작성 API
    @PostMapping
    @Operation(
            summary = "후기 작성",
            description = """
                    특정 택시팟에서 상대 슈니에게 후기를 작성합니다.
                    - 총대/동승 모두 작성 가능
                    - 단, 해당 택시팟의 멤버(총대 or ACCEPTED 동승)만 작성할 수 있습니다.
                    - 동일한 (택시팟, 작성자, 대상자) 조합으로는 한 번만 작성 가능합니다.
                    """
    )
    public ResponseEntity<Long> createReview(
            @AuthenticationPrincipal
            @Parameter(description = "현재 로그인한 사용자 정보 (JWT에서 복호화된 CustomUserDetails)")
            CustomUserDetails userDetails,

            @RequestBody
            @Parameter(description = "후기 작성 요청 바디", required = true)
            ReviewDto.CreateRequest request
    ) {
        // 현재 로그인 유저(리뷰 작성자)의 PK
        Long reviewerId = userDetails.getUserId();

        // 리뷰 생성 및 저장
        Long reviewId = reviewService.createReview(reviewerId, request);

        // HTTP 200 + 생성된 리뷰 ID 반환
        return ResponseEntity.ok(reviewId);
    }

    // 채팅방 -> "사용자 목록" 화면 데이터 조회 API
    @GetMapping("/members")
    @Operation(
            summary = "채팅방 사용자 목록용 후기 상태 조회",
            description = """
                    채팅방 하단 메뉴 → '사용자 목록' 화면에서 사용할 데이터를 조회합니다.
                    - 대상 택시팟 ID(taxiPartyId)를 쿼리 파라미터로 전달
                    - 응답에는 각 멤버의 프로필 정보 + 총대 여부 + '이미 후기 작성함 여부'가 포함됩니다.
                    """
    )
    public ResponseEntity<List<ReviewDto.MemberReviewStatus>> getMemberReviewStatusList(
            @AuthenticationPrincipal
            @Parameter(description = "현재 로그인한 사용자 정보 (JWT)", required = true)
            CustomUserDetails userDetails,

            @RequestParam("taxiPartyId")
            @Parameter(description = "대상 택시팟 ID", required = true, example = "9")
            Long taxiPartyId
    ) {
        Long currentUserId = userDetails.getUserId();

        // 서비스에서 현재 유저가 이 팟의 멤버인지 검증 및 목록 구성
        List<ReviewDto.MemberReviewStatus> result =
                reviewService.getMemberReviewStatusList(taxiPartyId, currentUserId);

        return ResponseEntity.ok(result);
    }

    // 프로필 요약 정보 조회 API
    @GetMapping("/profile/{userId}")
    @Operation(
            summary = "사용자 프로필 후기 요약 조회",
            description = """
                    특정 사용자가 지금까지 받은 후기들을 기반으로,
                    - 재매칭 희망률 (matchPreferenceRate)
                    - 미정산 이력 개수 (unpaidCount)
                    - 받은 매너 평가 태그별 카운트
                    - 받은 비매너 평가 태그별 카운트
                    를 조회합니다.
                    """
    )
    public ResponseEntity<ReviewDto.ProfileSummaryResponse> getProfileSummary(
            @PathVariable("userId")
            @Parameter(description = "프로필 대상 사용자 ID", required = true, example = "4")
            Long userId
    ) {
        ReviewDto.ProfileSummaryResponse response = reviewService.getProfileSummary(userId);
        return ResponseEntity.ok(response);
    }

    // 단일 후기 상세 조회 API
    @GetMapping("/{reviewId}")
    @Operation(
            summary = "단일 후기 상세 조회 (알림용)",
            description = """
                    후기 도착 알림을 클릭했을 때, 해당 리뷰 한 건의 상세 정보를 조회합니다.
                    - 현재 로그인한 유저는 반드시 이 리뷰의 '대상자(reviewee)'여야 합니다.
                    - 상단에는 리뷰 작성자의 재매칭 희망률 / 미정산 이력이 함께 내려갑니다.
                    - 본문에는 이 리뷰에서 받은 매너/비매너 태그 목록이 내려갑니다.
                    - canWriteBack=true 이면 '나도 후기 작성하러 가기' 버튼을 노출하면 됩니다.
                    """
    )
    public ResponseEntity<ReviewDto.SingleReviewDetailResponse> getSingleReviewDetail(
            @AuthenticationPrincipal
            @Parameter(description =  "현재 로그인한 사용자 정보 (JWT)", required = true)
            CustomUserDetails userDetails,

            @PathVariable("reviewId")
            @Parameter(description = "조회할 후기 ID", required = true, example = "12")
            Long reviewId
    ) {
        Long currentUserId = userDetails.getUserId();

        ReviewDto.SingleReviewDetailResponse response =
                reviewService.getSingleReviewDetail(reviewId, currentUserId);

        return ResponseEntity.ok(response);
    }
}
