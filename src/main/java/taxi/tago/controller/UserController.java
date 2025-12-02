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
import taxi.tago.util.JwtUtil;
import org.springframework.web.multipart.MultipartFile;
import taxi.tago.dto.*;
import taxi.tago.service.User.UserMapService;
import taxi.tago.service.User.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;


@RestController
@RequiredArgsConstructor
@Tag(name = "유저 마커 API", description = "유저 위치 및 마지막 활동 시간 업데이트, 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내) 기능을 제공합니다.")
public class UserController {

    private final UserMapService userMapService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    // 사용자 로그인
    @PostMapping("/api/login")
    @Operation(
            summary = "사용자 로그인",
            description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다."
    )
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new LoginResponse(
                        false,
                        "아이디를 입력해주세요.",
                        null,
                        null,
                        null
                ));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new LoginResponse(
                        false,
                        "비밀번호를 입력해주세요.",
                        request.getEmail(),
                        null,
                        null
                ));
            }

            // 로그인 처리
            var user = userService.login(request.getEmail(), request.getPassword());

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user);

            return ResponseEntity.ok(new LoginResponse(
                    true,
                    "로그인 성공",
                    user.getEmail(),
                    user.getRole(),
                    token
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new LoginResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null,
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new LoginResponse(
                    false,
                    "로그인 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null,
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
        userMapService.userMapUpdate(dto);
        return "유저 위치 및 활동시간 업데이트 성공";
    }

    // 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내)
    @GetMapping("/api/map")
    @Operation(
            summary = "현재 접속 중인 유저 조회",
            description = "마지막 활동 시간이 3분 이내인 모든 유저를 조회하여 지도 위에 마커로 띄웁니다."
    )
    public List<UserMapDto.Response> getActiveUsers(@RequestParam Long userId) {
        return userMapService.getActiveUsers(userId);
    }

    // 비밀번호 변경용 인증코드 발송
    @PostMapping("/api/password-reset/send-code")
    @Operation(
            summary = "비밀번호 변경용 인증코드 발송",
            description = "비밀번호 변경을 위한 이메일 인증 코드를 전송합니다."
    )
    public ResponseEntity<EmailAuthResponse> sendPasswordResetCode(@RequestBody EmailAuthRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null,
                        null
                ));
            }

            // 인증코드 발송
            userService.sendPasswordResetCode(request.getEmail());

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "인증 코드가 전송되었습니다.",
                    request.getEmail(),
                    null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new EmailAuthResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "인증 코드 전송에 실패했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

    // 비밀번호 변경용 인증코드 검증
    @PostMapping("/api/password-reset/verify-code")
    @Operation(
            summary = "비밀번호 변경용 인증코드 검증",
            description = "비밀번호 변경을 위해 전송된 이메일 인증 코드를 검증합니다."
    )
    public ResponseEntity<EmailAuthResponse> verifyPasswordResetCode(@RequestBody EmailAuthRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null,
                        null
                ));
            }

            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드를 입력해주세요.",
                        request.getEmail(),
                        null
                ));
            }

            // 인증코드 검증
            boolean isValid = userService.verifyPasswordResetCode(request.getEmail(), request.getCode());

            if (isValid) {
                return ResponseEntity.ok(new EmailAuthResponse(
                        true,
                        "인증 코드가 일치합니다.",
                        request.getEmail(),
                        null
                ));
            } else {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드가 일치하지 않습니다.",
                        request.getEmail(),
                        null
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new EmailAuthResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "인증 코드 검증 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

    // 비밀번호 변경용 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
    @PostMapping("/api/password-reset/resend-code")
    @Operation(
            summary = "비밀번호 변경용 인증코드 재전송",
            description = "기존 인증 코드를 초기화하고 새로운 인증 코드를 재전송합니다."
    )
    public ResponseEntity<EmailAuthResponse> resendPasswordResetCode(@RequestBody EmailAuthRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null,
                        null
                ));
            }

            // 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
            userService.resendPasswordResetCode(request.getEmail());

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "인증 코드가 재전송되었습니다. (기존 코드는 초기화되었습니다.)",
                    request.getEmail(),
                    null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new EmailAuthResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "인증 코드 재전송에 실패했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

    // 비밀번호 변경
    @PostMapping("/api/password-reset/change")
    @Operation(
            summary = "비밀번호 변경",
            description = "인증 코드 검증이 완료된 사용자의 비밀번호를 변경합니다."
    )
    public ResponseEntity<EmailAuthResponse> changePassword(@RequestBody PasswordResetRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null,
                        null
                ));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "비밀번호를 입력해주세요.",
                        request.getEmail(),
                        null
                ));
            }

            if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "비밀번호 확인을 입력해주세요.",
                        request.getEmail(),
                        null
                ));
            }

            // 비밀번호 변경 처리
            userService.changePassword(request.getEmail(), request.getPassword(), request.getConfirmPassword());

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "비밀번호가 성공적으로 변경되었습니다.",
                    request.getEmail(),
                    null
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new EmailAuthResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "비밀번호 변경 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

    // 마이페이지_프로필수정_기존정보조회
    @GetMapping("/api/users/info")
        @Operation(summary = "마이페이지_프로필수정", description = "유저의 현재 프로필 사진, 이름, 학번, 이메일을 반환합니다.")
    public MypageDto.InfoResponse getUserInfo(@RequestParam Long userId) {
        return userService.getUserInfo(userId);
    }

    // 마이페이지_프로필수정_프로필사진업로드
    @PutMapping(value = "/api/users/profile-image", consumes = "multipart/form-data")
    @Operation(summary = "프로필 사진 수정", description = "이미지 파일을 업로드하여 프로필 사진을 변경합니다.")
    public String updateProfileImage(
            @RequestParam Long userId,
            @RequestPart(value = "file") MultipartFile file
    ) {
        try {
            return userService.updateProfileImage(userId, file);
        } catch (IOException e) {
            // 파일 저장 중 에러가 나면 500 에러 반환
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}