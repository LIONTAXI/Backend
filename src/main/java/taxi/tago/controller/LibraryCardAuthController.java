package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import taxi.tago.dto.LibraryCardAuthResponse;
import taxi.tago.service.LibraryCardAuthService;
import taxi.tago.service.LibraryCardAuthService.LibraryCardAuthResult;

@RestController
@RequestMapping("/api/auth/library-card")
@RequiredArgsConstructor
public class LibraryCardAuthController {

    private final LibraryCardAuthService libraryCardAuthService;

    /**
     * 도서관 전자출입증 이미지 업로드 및 OCR 인식
     * @param imageFile 이미지 파일 (form-data에서 전송)
     * @return OCR 인식 결과 (이름, 학번 추출 결과)
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<LibraryCardAuthResponse> uploadLibraryCard(
            @RequestParam("image") MultipartFile imageFile) {
        
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
}

