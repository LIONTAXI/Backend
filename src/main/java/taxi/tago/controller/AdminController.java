package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.AuthRequestDto;
import taxi.tago.dto.AdminLoginRequest;
import taxi.tago.dto.AdminLoginResponse;
import taxi.tago.entity.LibraryCardAuth;
import taxi.tago.service.AdminService;
import taxi.tago.service.FileStorageService;
import taxi.tago.service.LibraryCardAuthService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LibraryCardAuthService libraryCardAuthService;
    private final FileStorageService fileStorageService;

    /**
     * 관리자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        try {
            // 입력값 검증
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new AdminLoginResponse(
                        false,
                        "이메일을 입력해주세요.",
                        null,
                        null
                ));
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new AdminLoginResponse(
                        false,
                        "비밀번호를 입력해주세요.",
                        request.getEmail(),
                        null
                ));
            }

            // 로그인 처리
            var user = adminService.login(request.getEmail(), request.getPassword());

            return ResponseEntity.ok(new AdminLoginResponse(
                    true,
                    "로그인 성공",
                    user.getEmail(),
                    user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AdminLoginResponse(
                    false,
                    e.getMessage(),
                    request.getEmail(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AdminLoginResponse(
                    false,
                    "로그인 처리 중 오류가 발생했습니다: " + e.getMessage(),
                    request.getEmail(),
                    null
            ));
        }
    }

    /**
     * 승인 대기 중인 모든 인증 요청 목록 조회
     */
    @GetMapping("/auth-requests")
    public ResponseEntity<List<AuthRequestDto>> getPendingAuthRequests() {
        try {
            List<LibraryCardAuth> authRequests = libraryCardAuthService.getPendingAuthRequests();
            List<AuthRequestDto> dtos = authRequests.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 인증 요청 상세 조회
     */
    @GetMapping("/auth-requests/{authId}")
    public ResponseEntity<AuthRequestDto> getAuthRequestDetail(@PathVariable(name = "authId") Long authId) {
        try {
            return libraryCardAuthService.getAuthRequestById(authId)
                    .map(auth -> ResponseEntity.ok(convertToDto(auth)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 인증 요청 이미지 조회
     */
    @GetMapping("/auth-requests/{authId}/image")
    public ResponseEntity<Resource> getAuthRequestImage(@PathVariable(name = "authId") Long authId) {
        try {
            return libraryCardAuthService.getAuthRequestById(authId)
                    .map(auth -> {
                        try {
                            if (auth.getImagePath() == null || auth.getImagePath().isEmpty()) {
                                return ResponseEntity.notFound().<Resource>build();
                            }
                            byte[] imageBytes = fileStorageService.loadImageFile(auth.getImagePath());
                            ByteArrayResource resource = new ByteArrayResource(imageBytes);
                            
                            // 이미지 타입 결정
                            String contentType = "image/jpeg";
                            String imagePath = auth.getImagePath().toLowerCase();
                            if (imagePath.endsWith(".png")) {
                                contentType = "image/png";
                            }
                            
                            return ResponseEntity.ok()
                                    .contentType(MediaType.parseMediaType(contentType))
                                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"image\"")
                                    .body((Resource) resource);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Resource>build();
                        }
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * LibraryCardAuth 엔티티를 DTO로 변환
     */
    private AuthRequestDto convertToDto(LibraryCardAuth auth) {
        AuthRequestDto dto = new AuthRequestDto();
        dto.setId(auth.getId());
        dto.setUserId(auth.getUser().getId());
        dto.setUserEmail(auth.getUser().getEmail());
        dto.setExtractedName(auth.getExtractedName());
        dto.setExtractedStudentId(auth.getExtractedStudentId());
        dto.setImagePath(auth.getImagePath());
        // 이미지 URL 생성
        if (auth.getImagePath() != null && !auth.getImagePath().isEmpty()) {
            dto.setImageUrl("/api/admin/auth-requests/" + auth.getId() + "/image");
        }
        dto.setStatus(auth.getStatus());
        dto.setCreatedAt(auth.getCreatedAt());
        dto.setReviewedAt(auth.getReviewedAt());
        dto.setFailureReason(auth.getFailureReason());
        return dto;
    }
}

