package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.UserMapDto;
import taxi.tago.service.UserMapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "유저 마커 API", description = "유저 위치 및 마지막 활동 시간 업데이트, 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내) 기능을 제공합니다.")
public class UserController {

    private final UserMapService userService;

    // 유저 위치 및 마지막 활동 시간 업데이트
    @PatchMapping("/api/map/user-map-update")
    @Operation(
            summary = "유저 위치 및 마지막 활동 시간 업데이트",
            description = "각 유저의 위치와 마지막으로 활동한 시간을 업데이트합니다."
    )
    public String userMapUpdate(@RequestBody UserMapDto.UpdateRequest dto) {
        userService.userMapUpdate(dto);
        return "유저 위치 및 활동시간 업데이트 성공";
    }

    // 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내)
    @GetMapping("/api/map")
    @Operation(
            summary = "현재 접속 중인 유저 조회",
            description = "마지막 활동 시간이 3분 이내인 모든 유저를 조회하여 지도 위에 마커로 띄웁니다."
    )
    public List<UserMapDto.Response> getActiveUsers() {
        return userService.getActiveUsers();
    }
}