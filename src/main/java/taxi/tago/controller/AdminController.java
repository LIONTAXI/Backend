package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.Admin.AuthRequestDto;
import taxi.tago.dto.Admin.ApproveRequestDto;
import taxi.tago.dto.Admin.AdminLoginRequest;
import taxi.tago.dto.Admin.AdminLoginResponse;
import taxi.tago.entity.LibraryCardAuth;
import taxi.tago.service.AdminService;
import taxi.tago.service.FileStorageService;
import taxi.tago.service.LibraryCardAuthService;
import taxi.tago.service.LibraryCardAuthService.LibraryCardAuthResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "관리자 API", description = "관리자(ADMIN) 전용 API입니다. 관리자 로그인 및 도서관 전자출입증 인증 요청 승인/반려 기능을 제공합니다. " +
        "관리자 계정은 환경변수로 관리되며 DB에 저장되지 않습니다. 일반 사용자 로그인은 /api/login 엔드포인트를 사용하세요.")
public class AdminController {

    private final AdminService adminService;
    private final LibraryCardAuthService libraryCardAuthService;
    private final FileStorageService fileStorageService;

    // 관리자 로그인
    @PostMapping("/login")
    @Operation(
            summary = "관리자 로그인",
            description = "관리자(ADMIN) 계정으로 로그인합니다. 환경변수(admin.email, admin.password)에 설정된 관리자 계정 정보로 인증합니다. " +
                    "관리자 계정은 DB에 저장되지 않으며, 환경변수로만 관리됩니다. " +
                    "일반 사용자 로그인은 /api/login 엔드포인트를 사용하세요."
    )
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

            // 로그인 처리 (환경변수 기반 인증)
            var adminInfo = adminService.login(request.getEmail(), request.getPassword());

            return ResponseEntity.ok(new AdminLoginResponse(
                    true,
                    "로그인 성공",
                    adminInfo.getEmail(),
                    adminInfo.getRole()
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

    // 승인 대기 중인 모든 인증 요청 목록 조회
    @GetMapping("/auth-requests")
    @Operation(
            summary = "승인 대기 중인 인증 요청 목록 조회",
            description = "승인 대기 상태인 모든 도서관 전자출입증 인증 요청을 조회합니다."
    )
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

    // 특정 인증 요청 상세 조회
    @GetMapping("/auth-requests/{authId}")
    @Operation(
            summary = "인증 요청 상세 조회",
            description = "특정 인증 요청의 상세 정보를 조회합니다."
    )
    public ResponseEntity<AuthRequestDto> getAuthRequestDetail(@PathVariable(name = "authId") Long authId) {
        try {
            return libraryCardAuthService.getAuthRequestById(authId)
                    .map(auth -> ResponseEntity.ok(convertToDto(auth)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 인증 요청 이미지 조회
    @GetMapping("/auth-requests/{authId}/image")
    @Operation(
            summary = "인증 요청 이미지 조회",
            description = "특정 인증 요청에 첨부된 도서관 전자출입증 이미지를 조회합니다."
    )
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

    // 반려 사유 목록 조회
    @GetMapping("/auth-requests/rejection-reasons")
    @Operation(
            summary = "반려 사유 목록 조회",
            description = "인증 요청 반려 시 사용할 수 있는 반려 사유 목록을 조회합니다."
    )
    public ResponseEntity<List<String>> getRejectionReasons() {
        try {
            List<String> reasons = List.of(
                "이미지와 입력 정보 불일치",
                "이미지 정보 미포함",
                "이미지 부정확"
            );
            return ResponseEntity.ok(reasons);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 인증 요청 승인/반려 처리
    @PostMapping("/auth-requests/approve")
    @Operation(
            summary = "인증 요청 승인/반려 처리",
            description = "도서관 전자출입증 인증 요청을 승인하거나 반려합니다. 반려 시 반려 사유를 필수로 입력해야 합니다."
    )
    public ResponseEntity<?> approveOrRejectAuthRequest(@RequestBody ApproveRequestDto request) {
        try {
            // 입력값 검증
            if (request.getAuthId() == null) {
                return ResponseEntity.badRequest().body("인증 요청 ID를 입력해주세요.");
            }

            if (request.getIsApproved() == null) {
                return ResponseEntity.badRequest().body("승인 또는 반려 여부를 선택해주세요.");
            }

            // 반려 시 반려 사유 필수
            if (!request.getIsApproved() && 
                (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
                return ResponseEntity.badRequest().body("반려 사유를 입력해주세요.");
            }

            // 승인/반려 처리
            LibraryCardAuthResult result = libraryCardAuthService.processApproval(
                request.getAuthId(),
                request.getIsApproved(),
                request.getRejectionReason()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(result.getMessage());
            } else {
                return ResponseEntity.badRequest().body(result.getMessage());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 승인/반려 완료된 인증 요청 목록 조회
    @GetMapping("/auth-requests/completed")
    @Operation(
            summary = "승인/반려 완료된 인증 요청 목록 조회",
            description = "승인 또는 반려가 완료된 인증 요청 목록을 조회합니다. status 파라미터로 필터링 가능합니다. (APPROVED, REJECTED)"
    )
    public ResponseEntity<List<AuthRequestDto>> getCompletedAuthRequests(
            @RequestParam(name = "status", required = false) String status) {
        try {
            List<LibraryCardAuth> authRequests;
            
            if (status != null && !status.trim().isEmpty()) {
                // 특정 상태로 필터링 (APPROVED 또는 REJECTED)
                if (status.equalsIgnoreCase("APPROVED")) {
                    authRequests = libraryCardAuthService.getApprovedAuthRequests();
                } else if (status.equalsIgnoreCase("REJECTED")) {
                    authRequests = libraryCardAuthService.getRejectedAuthRequests();
                } else {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                // status 파라미터가 없으면 승인과 반려 모두 조회
                List<LibraryCardAuth> approved = libraryCardAuthService.getApprovedAuthRequests();
                List<LibraryCardAuth> rejected = libraryCardAuthService.getRejectedAuthRequests();
                authRequests = new java.util.ArrayList<>();
                authRequests.addAll(approved);
                authRequests.addAll(rejected);
                // 최신순으로 정렬
                authRequests.sort((a, b) -> {
                    LocalDateTime dateA = a.getReviewedAt() != null ? a.getReviewedAt() : a.getCreatedAt();
                    LocalDateTime dateB = b.getReviewedAt() != null ? b.getReviewedAt() : b.getCreatedAt();
                    return dateB.compareTo(dateA);
                });
            }
            
            List<AuthRequestDto> dtos = authRequests.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // LibraryCardAuth 엔티티를 DTO로 변환
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

