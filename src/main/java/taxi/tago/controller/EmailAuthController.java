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
     * 
     * Response (성공 시):
     * {
     *   "success": true,
     *   "message": "인증이 완료되었습니다. 비밀번호를 설정해주세요.",
     *   "email": "student@swu.ac.kr"
     * }
     * 
     * Response (실패 시):
     * {
     *   "success": false,
     *   "message": "인증 코드가 일치하지 않습니다.",
     *   "email": "student@swu.ac.kr"
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<EmailAuthResponse> verifyAuthCode(@RequestBody EmailAuthRequest request) {
        try {
            // 이메일 입력 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null
                ));
            }
            
            // 인증 코드 입력 검증
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new EmailAuthResponse(
                        false,
                        "인증 코드를 입력해주세요.",
                        request.getEmail()
                ));
            }
            
            // 인증 코드 일치 여부 확인
            boolean isValid = emailAuthService.verifyAuthCode(request.getEmail(), request.getCode());
            
            if (isValid) {
                // 인증 성공: 비밀번호 설정 화면으로 이동 (아이디는 이메일로 자동 사용)
                return ResponseEntity.ok(new EmailAuthResponse(
                        true,
                        "인증이 완료되었습니다. 비밀번호를 설정해주세요.",
                        request.getEmail()  // 아이디로 사용할 웹 메일 반환
                ));
            } else {
                // 인증 실패: 에러 메시지 반환
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

