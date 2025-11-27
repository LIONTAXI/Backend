package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import taxi.tago.dto.TaxiPartyDto;
import taxi.tago.service.TaxiPartyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
}