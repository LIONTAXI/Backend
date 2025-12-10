package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import taxi.tago.dto.LibraryCard.LibraryCardAuthResponse;
import taxi.tago.service.LibraryCardAuthService;
import taxi.tago.service.LibraryCardAuthService.LibraryCardAuthResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth/library-card")
@RequiredArgsConstructor
@Tag(name = "도서관 전자출입증 인증 API", description = "도서관 전자출입증 이미지 업로드, OCR 인식 및 인증 요청 제출 기능을 제공합니다.")
public class LibraryCardAuthController {

    private final LibraryCardAuthService libraryCardAuthService;

    // 도서관 전자출입증 이미지 업로드 및 OCR 인식 (회원가입 플로우: 비밀번호 설정 완료 후)
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(
            summary = "도서관 전자출입증 이미지 업로드 및 OCR 인식",
            description = "도서관 전자출입증 이미지를 업로드하고 OCR을 통해 이름과 학번을 자동으로 추출합니다. 회원가입 플로우에서 비밀번호 설정 완료 후 사용하며, 이메일로 사용자를 찾아 학번과 이름을 업데이트합니다."
    )
    public ResponseEntity<LibraryCardAuthResponse> uploadLibraryCard(
            @RequestParam(name = "email", required = true) String email,
            @RequestParam(name = "image", required = true) MultipartFile imageFile) {
        
        try {
            // 이메일 입력 검증
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new LibraryCardAuthResponse(
                    false,
                    "이메일을 입력해주세요.",
                    null,
                    null
                ));
            }

            // 이미지 파일 검증
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new LibraryCardAuthResponse(
                    false,
                    "이미지를 등록해주세요.",
                    null,
                    null
                ));
            }

            // 이미지 파일 형식 검증
            String contentType = imageFile.getContentType();
            if (contentType == null || 
                (!contentType.startsWith("image/jpeg") && 
                 !contentType.startsWith("image/jpg") && 
                 !contentType.startsWith("image/png"))) {
                return ResponseEntity.badRequest().body(new LibraryCardAuthResponse(
                    false,
                    "이미지 파일만 업로드 가능합니다. (JPG, PNG)",
                    null,
                    null
                ));
            }

            // OCR 처리 및 사용자 정보 업데이트
            LibraryCardAuthResult result = libraryCardAuthService.processLibraryCardImageForRegistration(email, imageFile);

            LibraryCardAuthResponse response = new LibraryCardAuthResponse(
                result.isSuccess(),
                result.getMessage(),
                result.getExtractedName(),
                result.getExtractedStudentId()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new LibraryCardAuthResponse(
                false,
                "인증 처리 중 오류가 발생했습니다: " + e.getMessage(),
                null,
                null
            ));
        }
    }

    // 수동 인증 요청 제출 API
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    @Operation(
            summary = "수동 인증 요청 제출",
            description = "사진, 이름, 학번을 받아서 승인 대기 상태로 저장합니다. 관리자가 승인/반려 처리합니다."
    )
    public ResponseEntity<LibraryCardAuthResponse> submitManualAuth(
            @RequestParam(name = "userId", required = true) String userId, // 사용자 ID (이메일 또는 숫자 ID)
            @RequestParam(name = "image", required = true) MultipartFile imageFile, // 전자출입증 이미지 파일 (JPG, PNG)
            @RequestParam(name = "name", required = true) String name, // 사용자가 입력한 이름
            @RequestParam(name = "studentId", required = true) String studentId) { // 사용자가 입력한 학번 (10자리 숫자)
        
        try {
            // 이미지 파일 형식 검증
            if (imageFile != null && !imageFile.isEmpty()) {
                String contentType = imageFile.getContentType();
                if (contentType == null || 
                    (!contentType.startsWith("image/jpeg") && 
                     !contentType.startsWith("image/jpg") && 
                     !contentType.startsWith("image/png"))) {
                    return ResponseEntity.badRequest().body(new LibraryCardAuthResponse(
                        false,
                        "이미지 파일만 업로드 가능합니다. (JPG, PNG)",
                        null,
                        null
                    ));
                }
            }

            // 수동 인증 요청 제출
            LibraryCardAuthResult result = libraryCardAuthService.submitManualAuthRequest(
                userId, imageFile, name, studentId
            );

            LibraryCardAuthResponse response = new LibraryCardAuthResponse(
                result.isSuccess(),
                result.getMessage(),
                result.getExtractedName(),
                result.getExtractedStudentId()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new LibraryCardAuthResponse(
                false,
                "인증 요청 제출 중 오류가 발생했습니다: " + e.getMessage(),
                null,
                null
            ));
        }
    }
}

