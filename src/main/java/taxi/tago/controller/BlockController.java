package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.BlockDto;
import taxi.tago.service.BlockService;

import java.util.List;

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

    // 내가 차단한 목록
    @GetMapping("/api/blocks")
    @Operation(summary = "차단 목록 조회", description = "내가 차단한 사용자 목록을 조회합니다. 프로필 사진, 이름, 학번(2자리) 포함")
    public List<BlockDto.Response> getBlockList(@RequestParam Long blockerId) {
        return blockService.getBlockList(blockerId);
    }

    // 차단 해제
    @DeleteMapping("/api/blocks")
    @Operation(summary = "차단 해제하기", description = "차단했던 사용자를 차단 해제합니다.")
    public String unblockUser(@RequestBody BlockDto.BlockRequest dto) {
        return blockService.unblockUser(dto);
    }
}