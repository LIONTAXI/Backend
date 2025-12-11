package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.Email.EmailAuthRequest;
import taxi.tago.dto.Email.EmailAuthResponse;
import taxi.tago.dto.Email.EmailSendRequest;
import taxi.tago.dto.Password.PasswordSetRequest;
import taxi.tago.service.EmailAuthService;
import taxi.tago.service.LibraryCardAuthService;
import taxi.tago.service.LibraryCardAuthService.LibraryCardAuthInfo;
import taxi.tago.service.User.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
@Tag(name = "이메일 인증 API", description = "이메일 인증 코드 전송, 검증, 재전송 및 회원가입 기능을 제공합니다.")
public class EmailAuthController {

    private final EmailAuthService emailAuthService;
    private final UserService userService;
    private final LibraryCardAuthService libraryCardAuthService;

    // 인증 코드 전송
    @PostMapping("/send")
    @Operation(
            summary = "인증 코드 전송",
            description = "회원가입을 위한 이메일 인증 코드를 전송합니다."
    )
    public ResponseEntity<EmailAuthResponse> sendAuthCode(@RequestBody EmailSendRequest request) {
        try {
            emailAuthService.sendAuthCode(request.getEmail());
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

// 인증 코드 검증
    @PostMapping("/verify")
    @Operation(
            summary = "인증 코드 검증",
            description = "전송된 이메일 인증 코드를 검증합니다. 인증 성공 시 도서관 전자출입증 인증 화면으로 이동합니다."
    )
    public ResponseEntity<EmailAuthResponse> verifyAuthCode(@RequestBody EmailAuthRequest request) {
        try {
            // 이메일 입력 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null,
                        null
                ));
            }
            
            // 인증 코드 입력 검증
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드를 입력해주세요.",
                        request.getEmail(),
                        null
                ));
            }
            
            // 인증 코드 일치 여부 확인
            boolean isValid = emailAuthService.verifyAuthCode(request.getEmail(), request.getCode());
            
            if (isValid) {
                // 인증 성공: 비밀번호 설정 화면으로 이동
                return ResponseEntity.ok(new EmailAuthResponse(
                        true,
                        "인증이 완료되었습니다. 비밀번호를 설정해주세요.",
                        request.getEmail(),  // 아이디로 사용할 웹 메일 반환
                        null
                ));
            } else {
                // 인증 실패: 에러 메시지 반환
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
                    "인증 코드 검증에 실패했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

    //인증 코드 재전송 (코드 초기화 후 재전송)
    @PostMapping("/resend")
    @Operation(
            summary = "인증 코드 재전송",
            description = "기존 인증 코드를 초기화하고 새로운 인증 코드를 재전송합니다."
    )
    public ResponseEntity<EmailAuthResponse> resendAuthCode(@RequestBody EmailSendRequest request) {
        try {
            emailAuthService.resendAuthCode(request.getEmail());
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

    // 비밀번호 설정 및 회원가입 (이메일 인증 완료 후)
    @PostMapping(value = "/set-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "비밀번호 설정 및 회원가입",
            description = "이메일 인증이 완료된 사용자의 비밀번호를 설정하고 회원가입을 완료합니다. 도서관 전자출입증 인증은 이후 단계에서 진행됩니다."
    )
    public ResponseEntity<EmailAuthResponse> setPassword(@RequestBody PasswordSetRequest request) {
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

            // 비밀번호 임시 저장 (도서관 인증 완료 후 회원가입 처리)
            emailAuthService.savePasswordForRegistration(
                    request.getEmail(), 
                    request.getPassword(), 
                    request.getConfirmPassword()
            );

            return ResponseEntity.ok(new EmailAuthResponse(
                    true,
                    "비밀번호 설정이 완료되었습니다. 도서관 전자출입증을 등록해주세요.",
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
                    "회원가입에 실패했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }
}

