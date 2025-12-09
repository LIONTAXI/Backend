package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.SettlementDto;
import taxi.tago.security.CustomUserDetails;
import taxi.tago.service.SettlementService;

// 정산 관련 REST API 컨트롤러
// - /api/settlements : 정산 생성
// - /api/settlements/{settlementId} : 정산 상세 조회
// - /api/settlements/{id}/participants/{userId}/pay : 총대가 특정 참여자 정산 완료 처리
// - /api/settlements/{id}/remind : 총대가 미정산자에게 재촉(알림 + 채팅)
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Tag(
        name = "택시 정산 API",
        description = "택시팟 정산 생성, 상세 조회, 정산 완료 처리, 재촉 기능을 제공합니다."
)
public class SettlementController {

    private final SettlementService settlementService;

    // 정산 생성 API
    @PostMapping
    @Operation(
            summary = "정산 생성",
            description = """
                    택시팟에 대한 정산 정보를 생성합니다.
                    - 총대슈니만 호출할 수 있습니다.
                    - 택시 총 요금, 계좌 정보, 참여자별 금액 정보를 Body로 전달합니다.
                    - 이미 해당 택시팟에 정산이 존재하는 경우 400 에러를 반환합니다.
                    """
    )
    public ResponseEntity<Long> createSettlement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SettlementDto.CreateRequest request
    ) {
        Long hostId = userDetails.getUserId(); // 현재 로그인한 총대 유저
        Long settlementId = settlementService.createSettlement(hostId, request);
        return ResponseEntity.ok(settlementId); // HTTP 200 + body: 정산 ID
    }

    // 정산 상세 조회 API
    @GetMapping("/{settlementId}")
    @Operation(
            summary = "정산 상세 조회",
            description = """
                    정산 ID로 정산 상세 정보를 조회합니다.
                    - 정산에 포함된 총대/참여자만 조회할 수 있습니다.
                    - 택시 총 요금, 계좌 정보, 참여자별 금액/정산 상태를 반환합니다.
                    """
    )
    public ResponseEntity<SettlementDto.DetailResponse> getSettlementDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long settlementId
    ) {
        Long userId = userDetails.getUserId(); // 현재 로그인한 유저 ID
        SettlementDto.DetailResponse response = settlementService.getSettlementDetail(settlementId, userId);
        return ResponseEntity.ok(response); // HTTP 200 + 정산 상세 DTO
    }

    // 정산 완료 처리 API (총대 전용)
    @PostMapping("/{settlementId}/participants/{userId}/pay")
    @Operation(
            summary = "참여자 정산 완료 처리 (총대 전용)",
            description = """
                    총대슈니가 특정 참여자의 정산 상태를 '정산 완료'로 변경합니다.
                    - 호출자는 반드시 해당 정산의 총대여야 합니다.
                    - 이미 정산 완료된 경우에도 에러 없이 그대로 유지됩니다.
                    """
    )
    public ResponseEntity<Void> markPaid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long settlementId,
            @PathVariable("userId") Long targetUserId // path 변수 이름을 명시적으로 매핑
    ) {
        Long hostId = userDetails.getUserId(); // 현재 로그인한 총대 유저의 ID
        settlementService.markPaid(settlementId, hostId, targetUserId);
        return ResponseEntity.ok().build(); // HTTP 200, body 없음
    }

    // 정산 재촉 API (총대 전용)
    @PostMapping("/{settlementId}/remind")
    @Operation(
            summary = "정산 재촉 (총대 전용)",
            description = """
                    아직 정산하지 않은 동승슈니들에게 재촉 알림과 채팅 메시지를 전송합니다.
                    - 총대슈니만 호출할 수 있습니다.
                    - 마지막 재촉 시점으로부터 2시간 이내에는 다시 재촉할 수 없습니다.
                    """
    )
    public ResponseEntity<Void> remindUnpaid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long settlementId
    ) {
        Long hostId = userDetails.getUserId();
        settlementService.remindUnpaid(settlementId, hostId);
        return ResponseEntity.ok().build(); // HTTP 200, body 없음
    }

    // 이미 생성된 정산에 대해 "현재 로그인한 유저가 속해있는 settlementId"를 조회하는 API
    @GetMapping("/current")
    @Operation(
            summary = "현재 유저 기준 정산 ID 조회",
            description = """
                    특정 택시팟에 대해 이미 생성된 정산이 있는지, 있다면 그 정산 ID를 반환합니다.
                    - 쿼리 파라미터 taxiPartyId로 택시팟을 지정합니다.
                    - 현재 로그인한 유저가 해당 정산의 총대 또는 참여자인 경우에만 settlementId를 내려줍니다.
                    - 아직 정산이 생성되지 않은 경우 hasSettlement=false, settlementId=null 로 반환합니다.
                    """
    )
    public ResponseEntity<SettlementDto.SettlementResponse> getMySettlementId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("taxiPartyId") Long taxiPartyId
    ) {
        Long userId = userDetails.getUserId();

        SettlementDto.SettlementResponse response =
                settlementService.getMySettlementId(taxiPartyId, userId);

        return ResponseEntity.ok(response);
    }

}
