package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.Email.EmailAuthRequest;
import taxi.tago.dto.Email.EmailAuthResponse;
import taxi.tago.dto.Login.LoginRequest;
import taxi.tago.dto.Login.LoginResponse;
import taxi.tago.dto.Password.PasswordResetRequest;
import taxi.tago.dto.UserMapDto;
import taxi.tago.service.User.UserMapService;
import taxi.tago.service.User.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "유저 마커 API", description = "유저 위치 및 마지막 활동 시간 업데이트, 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내) 기능을 제공합니다.")
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

    // 비밀번호 변경용 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
    @PostMapping("/api/password-reset/resend-code")
    public ResponseEntity<EmailAuthResponse> resendPasswordResetCode(@RequestBody EmailAuthRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null
                ));
            }

            // 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
            authUserService.resendPasswordResetCode(request.getEmail());

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "인증 코드가 재전송되었습니다. (기존 코드는 초기화되었습니다.)",
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
                    "인증 코드 재전송에 실패했습니다: " + e.getMessage(),
                    request.getEmail()
            ));
        }
    }

    // 비밀번호 변경
    @PostMapping("/api/password-reset/change")
    public ResponseEntity<EmailAuthResponse> changePassword(@RequestBody PasswordResetRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null
                ));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "비밀번호를 입력해주세요.",
                        request.getEmail()
                ));
            }

            if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "비밀번호 확인을 입력해주세요.",
                        request.getEmail()
                ));
            }

            // 비밀번호 변경 처리
            authUserService.changePassword(request.getEmail(), request.getPassword(), request.getConfirmPassword());

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "비밀번호가 성공적으로 변경되었습니다.",
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
                    "비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail()
            ));
        }
    }
}