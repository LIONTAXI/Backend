package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.TaxiPartyDto;
import taxi.tago.dto.TaxiUserDto;
import taxi.tago.service.TaxiPartyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "택시팟 API", description = "택시팟 게시물 작성 기능을 제공합니다.")
public class TaxiPartyController {

    private final TaxiPartyService taxiPartyService;

    // 택시팟 게시물 작성
    @PostMapping("/api/taxi-party")
    @Operation(
            summary = "택시팟 게시물 작성",
            description = "택시팟 게시물을 작성하고 랜덤으로 이모지를 부여합니다."
    )
    public String createTaxiParty(@RequestBody TaxiPartyDto.CreateRequest dto) {
        Long id = taxiPartyService.createTaxiParty(dto);
        return "택시팟 생성 성공, ID: " + id;
    }

    // 택시팟 목록 조회
    @GetMapping("/api/taxi-party")
    @Operation(
            summary = "택시팟 목록 조회",
            description = "현재 상태가 '매칭 중'인 택시팟을 최신순으로 조회합니다."
    )
    public List<TaxiPartyDto.InfoResponse> getTaxiParties() {
        return taxiPartyService.getTaxiParties();
    }

    // 택시팟 정보
    @GetMapping("/api/taxi-party/{id}")
    public TaxiPartyDto.DetailResponse getTaxiPartyDetail(@PathVariable Long id, @RequestParam Long userId) {
        return taxiPartyService.getTaxiPartyDetail(id, userId);
    }

    // 택시팟 정보 - 동승슈니 - 같이 타기
    @PostMapping("/api/taxi-party/{partyId}/participation")
    public String applyTaxiParty(@PathVariable Long partyId, @RequestBody TaxiPartyDto.CreateRequest request) {
        return taxiPartyService.applyTaxiParty(partyId, request.getUserId());
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 조회
    @GetMapping("/api/taxi-party/{partyId}/requests")
    public List<TaxiUserDto.RequestResponse> getJoinRequests(@PathVariable Long partyId) {
        return taxiPartyService.getJoinRequests(partyId);
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 수락
    @PostMapping("/api/taxi-party/requests/{taxiUserId}/accept")
    public String acceptJoinRequest(@PathVariable Long taxiUserId) {
        return taxiPartyService.acceptJoinRequest(taxiUserId);
    }

    // 택시팟 상세페이지 - 총대슈니 - 매칭 종료
    @PostMapping("/api/taxi-party/{partyId}/close")
    public String closeTaxiParty(@PathVariable Long partyId, @RequestBody TaxiPartyDto.CreateRequest request) {
        return taxiPartyService.closeTaxiParty(partyId, request.getUserId());
    }
}