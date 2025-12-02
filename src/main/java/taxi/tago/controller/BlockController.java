package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import taxi.tago.dto.BlockDto;
import taxi.tago.service.BlockService;

@RestController
@RequiredArgsConstructor
@Tag(name = "차단 API", description = "사용자 차단 관리 기능")
public class BlockController {

    private final BlockService blockService;

    // 차단하기 API
    @PostMapping("/api/blocks")
    @Operation(summary = "사용자 차단하기", description = "특정 사용자를 차단합니다. 차단하면 서로의 게시글과 지도 마커가 보이지 않게 됩니다.")
    public String blockUser(@RequestBody BlockDto.BlockRequest dto) {
        return blockService.blockUser(dto);
    }
}