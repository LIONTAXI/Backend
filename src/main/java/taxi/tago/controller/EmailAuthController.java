package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.EmailAuthRequest;
import taxi.tago.dto.EmailAuthResponse;
import taxi.tago.service.EmailAuthService;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    /**
     * 1.1 회원가입 1차 인증 - 인증 코드 전송
     * POST /api/auth/email/send
     * 
     * Request Body:
     * {
     *   "email": "student@swu.ac.kr"
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<EmailAuthResponse> sendAuthCode(@RequestBody EmailAuthRequest request) {
        try {
            emailAuthService.sendAuthCode(request.getEmail());
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

    /**
     * 인증 코드 검증
     * POST /api/auth/email/verify
     * 
     * Request Body:
     * {
     *   "email": "student@swu.ac.kr",
     *   "code": "123456"
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<EmailAuthResponse> verifyAuthCode(@RequestBody EmailAuthRequest request) {
        try {
            boolean isValid = emailAuthService.verifyAuthCode(request.getEmail(), request.getCode());
            
            if (isValid) {
                return ResponseEntity.ok(new EmailAuthResponse(
                        true,
                        "인증이 완료되었습니다.",
                        request.getEmail()
                ));
            } else {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드가 일치하지 않거나 만료되었습니다.",
                        request.getEmail()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EmailAuthResponse(
                    false,
                    "인증 코드 검증에 실패했습니다: " + e.getMessage(),
                    request.getEmail()
            ));
        }
    }

    /**
     * 인증 코드 재전송 (코드 초기화 후 재전송)
     * POST /api/auth/email/resend
     * 
     * Request Body:
     * {
     *   "email": "student@swu.ac.kr"
     * }
     */
    @PostMapping("/resend")
    public ResponseEntity<EmailAuthResponse> resendAuthCode(@RequestBody EmailAuthRequest request) {
        try {
            emailAuthService.resendAuthCode(request.getEmail());
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
}

