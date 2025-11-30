package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import taxi.tago.dto.LibraryCard.LibraryCardAuthResponse;
import taxi.tago.service.LibraryCardAuthService;
import taxi.tago.service.LibraryCardAuthService.LibraryCardAuthResult;

@RestController
@RequestMapping("/api/auth/library-card")
@RequiredArgsConstructor
public class LibraryCardAuthController {

    private final LibraryCardAuthService libraryCardAuthService;

    /**
     * 도서관 전자출입증 이미지 업로드 및 OCR 인식
     * 
     * @param imageFile 전자출입증 이미지 파일 (JPG, PNG) - 필수 파라미터
     * @return OCR 인식 결과 (이름, 학번 추출 결과)
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<LibraryCardAuthResponse> uploadLibraryCard(
            @RequestParam(name = "image", required = true) MultipartFile imageFile) {
        
        try {
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

            // OCR 처리
            LibraryCardAuthResult result = libraryCardAuthService.processLibraryCardImage(imageFile);

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

    /**
     * 수동 인증 요청 제출 (사진, 이름, 학번을 받아서 승인 대기 상태로 저장)
     * 
     * @param userId 사용자 ID (이메일 또는 숫자 ID) - 필수 파라미터
     * @param imageFile 전자출입증 이미지 파일 (JPG, PNG) - 필수 파라미터
     * @param name 사용자가 입력한 이름 - 필수 파라미터
     * @param studentId 사용자가 입력한 학번 (10자리 숫자) - 필수 파라미터
     * @return 인증 요청 결과
     */
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    public ResponseEntity<LibraryCardAuthResponse> submitManualAuth(
            @RequestParam(name = "userId", required = true) String userId,
            @RequestParam(name = "image", required = true) MultipartFile imageFile,
            @RequestParam(name = "name", required = true) String name,
            @RequestParam(name = "studentId", required = true) String studentId) {
        
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

