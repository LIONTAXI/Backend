package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.UserMapDto;
import taxi.tago.service.UserMapService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserMapService userService;

    // 유저 위치 및 마지막 활동 시간 업데이트
    @PatchMapping("/api/map/user-map-update")
    public String userMapUpdate(@RequestBody UserMapDto.UpdateRequest dto) {
        userService.userMapUpdate(dto);
        return "유저 위치 및 활동시간 업데이트 성공";
    }

    // 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내)
    @GetMapping("/api/map")
    public List<UserMapDto.Response> getActiveUsers() {
        return userService.getActiveUsers();
    }
}