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
    public List<TaxiPartyDto.InfoResponse> getTaxiParties(@RequestParam(name = "userId") Long userId) {
        return taxiPartyService.getTaxiParties(userId);
    }

    // 택시팟 정보
    @GetMapping("/api/taxi-party/{id}")
    @Operation(
            summary = "택시팟 정보",
            description = "해당 택시팟의 상세정보를 표시합니다. 총대슈니/동승슈니 공통으로 사용, 추가설명 전문 포함"
    )
    public TaxiPartyDto.DetailResponse getTaxiPartyDetail(@PathVariable(name = "id") Long id, @RequestParam(name = "userId") Long userId) {
        return taxiPartyService.getTaxiPartyDetail(id, userId);
    }

    // 택시팟 정보 - 동승슈니 - 같이 타기
    @PostMapping("/api/taxi-party/{partyId}/participation")
    @Operation(
            summary = "동승슈니 - 같이 타기",
            description = "택시팟 정보 페이지에서 동승슈니가 같이 타기 요청을 보냅니다."
    )
    public String applyTaxiParty(@PathVariable(name = "partyId") Long partyId, @RequestBody TaxiPartyDto.CreateRequest request) {
        return taxiPartyService.applyTaxiParty(partyId, request.getUserId());
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 조회
    @GetMapping("/api/taxi-party/{partyId}/requests")
    @Operation(
            summary = "총대슈니 - 택시팟 참여 요청 조회",
            description = "총대슈니가 해당 택시팟 글에 들어온 모든 같이 타기 요청을 확인합니다. 수락 이후에도 목록에 표시됩니다."
    )
    public List<TaxiUserDto.RequestResponse> getJoinRequests(@PathVariable(name = "partyId") Long partyId) {
        return taxiPartyService.getJoinRequests(partyId);
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 수락
    @PostMapping("/api/taxi-party/requests/{taxiUserId}/accept")
    @Operation(
            summary = "총대슈니 - 택시팟 참여 요청 수락",
            description = "총대슈니가 특정 동승슈니에 대해 같이 타기 요청을 수락합니다. 해당 동승슈니는 이제 채팅방에 입장할 수 있습니다."
    )
    public String acceptJoinRequest(@PathVariable(name = "taxiUserId") Long taxiUserId) {
        return taxiPartyService.acceptJoinRequest(taxiUserId);
    }

    // 택시팟 상세페이지 - 총대슈니 - 매칭 종료
    @PostMapping("/api/taxi-party/{partyId}/close")
    @Operation(
            summary = "총대슈니 - 매칭 종료",
            description = "총대슈니가 매칭을 종료합니다. 해당 택시팟에 더이상 새로운 동승슈니가 함께할 수 없습니다."
    )
    public String closeTaxiParty(@PathVariable(name = "partyId") Long partyId, @RequestBody TaxiPartyDto.CreateRequest request) {
        return taxiPartyService.closeTaxiParty(partyId, request.getUserId());
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 삭제
    @DeleteMapping("/api/taxi-party/{partyId}")
    @Operation(
            summary = "총대슈니 - 택시팟 삭제",
            description = "총대슈니가 택시팟을 삭제합니다. 해당 택시팟에 동승슈니가 없을 때만 삭제할 수 있습니다."
    )
    public String deleteTaxiParty(@PathVariable(name = "partyId") Long partyId, @RequestBody TaxiPartyDto.CreateRequest request) {
        return taxiPartyService.deleteTaxiParty(partyId, request.getUserId());
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 수정
    @PutMapping("/api/taxi-party/{partyId}")
    @Operation(
            summary = "총대슈니 - 택시팟 수정",
            description = "총대슈니가 본인의 택시팟 게시물을 수정합니다."
    )
    public String updateTaxiParty(@PathVariable(name = "partyId") Long partyId, @RequestBody TaxiPartyDto.UpdateRequest dto) {
        return taxiPartyService.updateTaxiParty(partyId, dto);
    }
}