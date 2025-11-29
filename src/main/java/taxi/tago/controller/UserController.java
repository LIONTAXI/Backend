package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.EmailAuthRequest;
import taxi.tago.dto.EmailAuthResponse;
import taxi.tago.dto.LoginRequest;
import taxi.tago.dto.LoginResponse;
import taxi.tago.dto.UserMapDto;
import taxi.tago.service.UserMapService;
import taxi.tago.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserMapService userService;
    private final UserService authUserService;

    // 사용자 로그인
    @PostMapping("/api/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new LoginResponse(
                        false,
                        "아이디를 입력해주세요.",
                        null,
                        null
                ));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new LoginResponse(
                        false,
                        "비밀번호를 입력해주세요.",
                        request.getEmail(),
                        null
                ));
            }

            // 로그인 처리
            var user = authUserService.login(request.getEmail(), request.getPassword());

            return ResponseEntity.ok(new LoginResponse(
                    true,
                    "로그인 성공",
                    user.getEmail(),
                    user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new LoginResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new LoginResponse(
                    false,
                    "로그인 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

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

    // 비밀번호 변경용 인증코드 발송
    @PostMapping("/api/password-reset/send-code")
    public ResponseEntity<EmailAuthResponse> sendPasswordResetCode(@RequestBody EmailAuthRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null
                ));
            }

            // 인증코드 발송
            authUserService.sendPasswordResetCode(request.getEmail());

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "인증 코드가 전송되었습니다.",
                    request.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new EmailAuthResponse(
                    false,
                    e.getMessage(),
                    request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "인증 코드 전송에 실패했습니다: " + e.getMessage(),
                    request.getEmail()
            ));
        }
    }

    // 비밀번호 변경용 인증코드 검증
    @PostMapping("/api/password-reset/verify-code")
    public ResponseEntity<EmailAuthResponse> verifyPasswordResetCode(@RequestBody EmailAuthRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null
                ));
            }

            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드를 입력해주세요.",
                        request.getEmail()
                ));
            }

            // 인증코드 검증
            boolean isValid = authUserService.verifyPasswordResetCode(request.getEmail(), request.getCode());

            if (isValid) {
                return ResponseEntity.ok(new EmailAuthResponse(
                        true,
                        "인증 코드가 일치합니다.",
                        request.getEmail()
                ));
            } else {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드가 일치하지 않습니다.",
                        request.getEmail()
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new EmailAuthResponse(
                    false,
                    e.getMessage(),
                    request.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "인증 코드 검증 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail()
            ));
        }
    }
}