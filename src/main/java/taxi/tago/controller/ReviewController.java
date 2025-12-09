package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import taxi.tago.dto.ReviewDto;
import taxi.tago.security.CustomUserDetails;
import taxi.tago.service.ReviewService;

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
}
