package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import taxi.tago.constant.AuthStatus;
import taxi.tago.entity.LibraryCardAuth;
import taxi.tago.entity.User;
import taxi.tago.repository.LibraryCardAuthRepository;
import taxi.tago.repository.UserRepository;
import taxi.tago.service.NaverOcrService.OcrResult;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryCardAuthService {

    private final NaverOcrService ocrService;
    private final UserRepository userRepository;
    private final LibraryCardAuthRepository libraryCardAuthRepository;
    private final FileStorageService fileStorageService;
    private final EmailAuthService emailAuthService;
    private final taxi.tago.service.User.UserService userService;

    // 서울여대 학번 패턴: 10자리 숫자 (예: 2021111222)
    private static final Pattern SWU_STUDENT_ID_PATTERN = Pattern.compile("^\\d{10}$");
    
    // 이메일별 도서관 인증 정보 임시 저장 (회원가입 전용, 30분 유효)
    private final Map<String, LibraryCardAuthInfo> libraryCardAuthStorage = new ConcurrentHashMap<>();
    
    // 도서관 인증 정보를 저장하는 내부 클래스
    public static class LibraryCardAuthInfo {
        private final String name;
        private final String studentId;
        private final LocalDateTime createdAt;
        
        public LibraryCardAuthInfo(String name, String studentId, LocalDateTime createdAt) {
            this.name = name;
            this.studentId = studentId;
            this.createdAt = createdAt;
        }
        
        public String getName() {
            return name;
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public boolean isExpired(int expirationMinutes) {
            return LocalDateTime.now().isAfter(createdAt.plusMinutes(expirationMinutes));
        }
    }

    // 도서관 전자출입증 이미지를 OCR로 처리 (회원가입 플로우: 비밀번호 설정 완료 후 사용자 정보 업데이트)
    @Transactional
    public LibraryCardAuthResult processLibraryCardImageForRegistration(String email, MultipartFile imageFile) {
        try {
            // 1. 이미지 파일 검증
            if (imageFile == null || imageFile.isEmpty()) {
                return LibraryCardAuthResult.failure("이미지를 등록해주세요.");
            }

            // 2. 임시 저장된 비밀번호 조회 (비밀번호 설정 단계에서 저장됨)
            EmailAuthService.PasswordInfo passwordInfo = emailAuthService.getAndRemovePasswordForRegistration(email);
            if (passwordInfo == null) {
                return LibraryCardAuthResult.failure("비밀번호 설정이 완료되지 않았습니다. 먼저 비밀번호를 설정해주세요.");
            }

            // 3. 이미 가입된 이메일인지 확인
            if (userRepository.findByEmailAndRole(email, taxi.tago.constant.UserRole.USER).isPresent()) {
                return LibraryCardAuthResult.failure("이미 가입된 이메일입니다.");
            }

            // 3. OCR로 이미지에서 텍스트 추출
            byte[] imageBytes = imageFile.getBytes();
            OcrResult ocrResult = ocrService.extractText(imageBytes);

            String extractedName = ocrResult.getName();
            String extractedStudentId = ocrResult.getStudentId();

            // 4. 이름 또는 학번이 추출되지 않은 경우
            if (extractedName == null || extractedName.trim().isEmpty()) {
                return LibraryCardAuthResult.failureWithDetails(
                    "이미지에서 이름을 찾을 수 없습니다. 명확한 사진을 다시 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            if (extractedStudentId == null || extractedStudentId.trim().isEmpty()) {
                return LibraryCardAuthResult.failureWithDetails(
                    "이미지에서 학번을 찾을 수 없습니다. 명확한 사진을 다시 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            // 5. 서울여대 학번 형식 검증
            if (!isValidSwuStudentId(extractedStudentId)) {
                return LibraryCardAuthResult.failureWithDetails(
                    "서울여대 학번 형식이 아닙니다. 올바른 전자출입증을 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            // 6. 완전한 회원가입 처리 (비밀번호, 학번, 이름 모두 포함)
            User user = userService.completeRegistration(
                    email,
                    passwordInfo.getPassword(),
                    extractedStudentId.trim(),
                    extractedName.trim()
            );

            // 7. 인증 기록 저장
            LibraryCardAuth auth = createAuthRecord(user, extractedName.trim(), extractedStudentId.trim(), true, null);
            libraryCardAuthRepository.save(auth);

            log.info("도서관 전자출입증 인증 완료 및 회원가입 완료: email={}, name={}, studentId={}", 
                email, extractedName, extractedStudentId);

            return LibraryCardAuthResult.success(
                "인증 완료! 회원가입이 완료되었습니다.",
                extractedName, extractedStudentId
            );

        } catch (IOException e) {
            log.error("이미지 파일 읽기 오류", e);
            return LibraryCardAuthResult.failure("이미지 파일을 읽는 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("도서관 전자출입증 OCR 처리 중 오류 발생", e);
            return LibraryCardAuthResult.failure("OCR 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 이메일로 저장된 도서관 인증 정보 조회 (회원가입 시 사용, 없거나 만료된 경우 null 반환)
    public LibraryCardAuthInfo getLibraryCardAuthInfo(String email) {
        LibraryCardAuthInfo authInfo = libraryCardAuthStorage.get(email);
        if (authInfo == null) {
            return null;
        }
        
        // 만료 확인 (30분)
        if (authInfo.isExpired(30)) {
            libraryCardAuthStorage.remove(email);
            log.warn("도서관 전자출입증 인증 정보 만료: email={}", email);
            return null;
        }
        
        return authInfo;
    }
    
    // 이메일로 저장된 도서관 인증 정보 제거 (회원가입 완료 후)
    public void removeLibraryCardAuthInfo(String email) {
        libraryCardAuthStorage.remove(email);
        log.info("도서관 전자출입증 인증 정보 제거: email={}", email);
    }

    // 도서관 전자출입증 이미지를 업로드하고 인증 처리 (기존 메서드 - 하위 호환성 유지)
    @Transactional
    public LibraryCardAuthResult authenticateLibraryCard(String userId, MultipartFile imageFile) {
        try {
            // 1. 이미지 파일 검증
            if (imageFile == null || imageFile.isEmpty()) {
                return LibraryCardAuthResult.failure("이미지를 등록해주세요.");
            }

            // 2. 사용자 조회
            Optional<User> userOpt = findUserByIdentifier(userId);
            if (userOpt.isEmpty()) {
                return LibraryCardAuthResult.failure("사용자를 찾을 수 없습니다.");
            }
            User user = userOpt.get();

            // 3. OCR로 이미지에서 텍스트 추출
            byte[] imageBytes = imageFile.getBytes();
            OcrResult ocrResult = ocrService.extractText(imageBytes);

            String extractedName = ocrResult.getName();
            String extractedStudentId = ocrResult.getStudentId();

            // 4. 이름 또는 학번이 추출되지 않은 경우
            if (extractedName == null || extractedName.trim().isEmpty()) {
                LibraryCardAuth auth = createAuthRecord(user, extractedName, extractedStudentId, false, 
                    "이미지에서 이름을 찾을 수 없습니다. 명확한 사진을 다시 등록해주세요.");
                libraryCardAuthRepository.save(auth);
                return LibraryCardAuthResult.failureWithDetails(
                    "이미지에서 이름을 찾을 수 없습니다. 명확한 사진을 다시 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            if (extractedStudentId == null || extractedStudentId.trim().isEmpty()) {
                LibraryCardAuth auth = createAuthRecord(user, extractedName, extractedStudentId, false, 
                    "이미지에서 학번을 찾을 수 없습니다. 명확한 사진을 다시 등록해주세요.");
                libraryCardAuthRepository.save(auth);
                return LibraryCardAuthResult.failureWithDetails(
                    "이미지에서 학번을 찾을 수 없습니다. 명확한 사진을 다시 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            // 5. 서울여대 학번 형식 검증
            if (!isValidSwuStudentId(extractedStudentId)) {
                LibraryCardAuth auth = createAuthRecord(user, extractedName, extractedStudentId, false, 
                    "서울여대 학번 형식이 아닙니다. 올바른 전자출입증을 등록해주세요.");
                libraryCardAuthRepository.save(auth);
                return LibraryCardAuthResult.failureWithDetails(
                    "서울여대 학번 형식이 아닙니다. 올바른 전자출입증을 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            // 6. 사용자 정보와 비교
            // 사용자가 이미 등록한 이름/학번이 있는 경우 비교
            String userStudentId = user.getStudentId();
            String userName = user.getName();

            // 사용자 정보가 이미 있는 경우 비교
            if (userStudentId != null && !userStudentId.trim().isEmpty()) {
                if (!extractedStudentId.equals(userStudentId)) {
                    LibraryCardAuth auth = createAuthRecord(user, extractedName, extractedStudentId, false, 
                        "등록된 학번과 일치하지 않습니다.");
                    libraryCardAuthRepository.save(auth);
                    return LibraryCardAuthResult.failureWithDetails(
                        "등록된 학번과 일치하지 않습니다.",
                        extractedName, extractedStudentId
                    );
                }
            }

            if (userName != null && !userName.trim().isEmpty()) {
                // 이름 비교 (공백 제거 후 비교)
                String normalizedExtractedName = normalizeName(extractedName);
                String normalizedUserName = normalizeName(userName);
                if (!normalizedExtractedName.equals(normalizedUserName)) {
                    LibraryCardAuth auth = createAuthRecord(user, extractedName, extractedStudentId, false, 
                        "등록된 이름과 일치하지 않습니다.");
                    libraryCardAuthRepository.save(auth);
                    return LibraryCardAuthResult.failureWithDetails(
                        "등록된 이름과 일치하지 않습니다.",
                        extractedName, extractedStudentId
                    );
                }
            }

            // 7. 인증 성공 - 사용자 정보에 학번과 이름 저장
            user.setStudentId(extractedStudentId);
            if (userName == null || userName.trim().isEmpty()) {
                user.setName(extractedName);
            }
            userRepository.save(user);

            // 8. 인증 기록 저장
            LibraryCardAuth auth = createAuthRecord(user, extractedName, extractedStudentId, true, null);
            libraryCardAuthRepository.save(auth);

            log.info("도서관 전자출입증 인증 성공: userId={}, name={}, studentId={}", 
                userId, extractedName, extractedStudentId);

            return LibraryCardAuthResult.success(
                "인증 완료!",
                extractedName, extractedStudentId
            );

        } catch (IOException e) {
            log.error("이미지 파일 읽기 오류", e);
            return LibraryCardAuthResult.failure("이미지 파일을 읽는 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("도서관 전자출입증 인증 중 오류 발생", e);
            return LibraryCardAuthResult.failure("인증 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //사용자 식별자로 사용자 찾기 (이메일 또는 ID)
    private Optional<User> findUserByIdentifier(String identifier) {
        // 숫자로만 이루어진 경우 ID로 조회
        if (identifier.matches("^\\d+$")) {
            return userRepository.findById(Long.parseLong(identifier));
        }
        // 그 외의 경우 이메일로 조회
        return userRepository.findByEmail(identifier);
    }

    //서울여대 학번 형식 검증
    private boolean isValidSwuStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        // 10자리 숫자인지 확인
        return SWU_STUDENT_ID_PATTERN.matcher(studentId.trim()).matches();
    }

    //이름 정규화 (공백 제거, 공백 하나로 통일)
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", "");
    }

    // 수동 인증 요청 제출 (사진, 이름, 학번을 받아서 승인 대기 상태로 저장)
    @Transactional
    public LibraryCardAuthResult submitManualAuthRequest(String userId, MultipartFile imageFile, 
                                                         String name, String studentId) {
        try {
            // 1. 입력값 검증
            if (imageFile == null || imageFile.isEmpty()) {
                return LibraryCardAuthResult.failure("이미지를 등록해주세요.");
            }

            if (name == null || name.trim().isEmpty()) {
                return LibraryCardAuthResult.failure("이름을 입력해주세요.");
            }

            if (studentId == null || studentId.trim().isEmpty()) {
                return LibraryCardAuthResult.failure("학번을 입력해주세요.");
            }

            // 2. 학번 형식 검증
            if (!isValidSwuStudentId(studentId)) {
                return LibraryCardAuthResult.failure("서울여대 학번 형식이 아닙니다. (10자리 숫자)");
            }

            // 3. 사용자 조회
            Optional<User> userOpt = findUserByIdentifier(userId);
            if (userOpt.isEmpty()) {
                return LibraryCardAuthResult.failure("사용자를 찾을 수 없습니다.");
            }
            User user = userOpt.get();

            // 4. 이미지 파일 저장
            String imagePath = fileStorageService.saveImageFile(imageFile);

            // 5. 승인 대기 상태로 인증 요청 저장
            LibraryCardAuth auth = new LibraryCardAuth();
            auth.setUser(user);
            auth.setExtractedName(name.trim());
            auth.setExtractedStudentId(studentId.trim());
            auth.setImagePath(imagePath);
            auth.setStatus(AuthStatus.PENDING);
            auth.setIsSuccess(false); // 아직 승인되지 않음
            libraryCardAuthRepository.save(auth);

            log.info("수동 인증 요청 제출: userId={}, name={}, studentId={}", userId, name, studentId);

            return LibraryCardAuthResult.success(
                "인증 요청이 제출되었습니다. 관리자 승인을 기다려주세요.",
                name, studentId
            );

        } catch (IOException e) {
            log.error("이미지 파일 저장 오류", e);
            return LibraryCardAuthResult.failure("이미지 파일 저장 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("수동 인증 요청 제출 중 오류 발생", e);
            return LibraryCardAuthResult.failure("인증 요청 제출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 승인 대기 중인 모든 인증 요청 목록 조회
    public List<LibraryCardAuth> getPendingAuthRequests() {
        return libraryCardAuthRepository.findByStatusOrderByCreatedAtDesc(AuthStatus.PENDING);
    }

    // 특정 인증 요청 상세 조회
    public Optional<LibraryCardAuth> getAuthRequestById(Long authId) {
        return libraryCardAuthRepository.findById(authId);
    }

    // 인증 요청 승인/반려 처리 (isApproved: true=승인, false=반려, rejectionReason: 반려 시 필수)
    @Transactional
    public LibraryCardAuthResult processApproval(Long authId, boolean isApproved, String rejectionReason) {
        try {
            // 1. 인증 요청 조회
            Optional<LibraryCardAuth> authOpt = libraryCardAuthRepository.findById(authId);
            if (authOpt.isEmpty()) {
                return LibraryCardAuthResult.failure("인증 요청을 찾을 수 없습니다.");
            }
            LibraryCardAuth auth = authOpt.get();

            // 2. 이미 처리된 요청인지 확인
            if (auth.getStatus() != AuthStatus.PENDING) {
                return LibraryCardAuthResult.failure("이미 처리된 인증 요청입니다.");
            }

            // 3. 반려 시 반려 사유 검증
            if (!isApproved) {
                if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                    return LibraryCardAuthResult.failure("반려 사유를 입력해주세요.");
                }
                
                // 반려 사유가 유효한지 확인 (3가지 중 하나)
                List<String> validReasons = List.of(
                    "이미지와 입력 정보 불일치",
                    "이미지 정보 미포함",
                    "이미지 부정확"
                );
                if (!validReasons.contains(rejectionReason)) {
                    return LibraryCardAuthResult.failure("유효하지 않은 반려 사유입니다.");
                }
            }

            // 4. 승인 처리
            if (isApproved) {
                auth.setStatus(AuthStatus.APPROVED);
                auth.setIsSuccess(true);
                auth.setFailureReason(null);
                auth.setReviewedAt(LocalDateTime.now());
                
                // 사용자 정보 업데이트 (2차 회원가입 완료 처리)
                User user = auth.getUser();
                if (auth.getExtractedStudentId() != null) {
                    user.setStudentId(auth.getExtractedStudentId());
                }
                if (auth.getExtractedName() != null) {
                    user.setName(auth.getExtractedName());
                }
                userRepository.save(user);
                
                log.info("인증 요청 승인 완료: authId={}, userId={}", authId, user.getId());
                
                return LibraryCardAuthResult.success(
                    "인증 요청이 승인되었습니다.",
                    auth.getExtractedName(),
                    auth.getExtractedStudentId()
                );
            } 
            // 5. 반려 처리
            else {
                auth.setStatus(AuthStatus.REJECTED);
                auth.setIsSuccess(false);
                auth.setFailureReason(rejectionReason);
                auth.setReviewedAt(LocalDateTime.now());
                libraryCardAuthRepository.save(auth);
                
                // 반려 메일 전송 (1차 회원가입 시 사용한 웹메일로)
                User user = auth.getUser();
                String userEmail = user.getEmail();
                emailAuthService.sendRejectionEmail(userEmail, rejectionReason);
                
                log.info("인증 요청 반려 완료: authId={}, userId={}, reason={}", 
                    authId, user.getId(), rejectionReason);
                
                return LibraryCardAuthResult.failure("인증 요청이 반려되었습니다.");
            }

        } catch (Exception e) {
            log.error("인증 요청 승인/반려 처리 중 오류 발생", e);
            return LibraryCardAuthResult.failure("처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 승인된 인증 요청 목록 조회
    public List<LibraryCardAuth> getApprovedAuthRequests() {
        return libraryCardAuthRepository.findByStatusOrderByCreatedAtDesc(AuthStatus.APPROVED);
    }

    // 반려된 인증 요청 목록 조회
    public List<LibraryCardAuth> getRejectedAuthRequests() {
        return libraryCardAuthRepository.findByStatusOrderByCreatedAtDesc(AuthStatus.REJECTED);
    }

    //인증 기록 생성
    private LibraryCardAuth createAuthRecord(User user, String extractedName, String extractedStudentId, 
                                            boolean isSuccess, String failureReason) {
        LibraryCardAuth auth = new LibraryCardAuth();
        auth.setUser(user);
        auth.setExtractedName(extractedName);
        auth.setExtractedStudentId(extractedStudentId);
        auth.setIsSuccess(isSuccess);
        auth.setFailureReason(failureReason);
        auth.setStatus(isSuccess ? AuthStatus.APPROVED : AuthStatus.REJECTED);
        return auth;
    }

    //인증 결과를 담는 클래스
    public static class LibraryCardAuthResult {
        private final boolean success;
        private final String message;
        private final String extractedName;
        private final String extractedStudentId;

        private LibraryCardAuthResult(boolean success, String message, String extractedName, String extractedStudentId) {
            this.success = success;
            this.message = message;
            this.extractedName = extractedName;
            this.extractedStudentId = extractedStudentId;
        }

        public static LibraryCardAuthResult success(String message, String extractedName, String extractedStudentId) {
            return new LibraryCardAuthResult(true, message, extractedName, extractedStudentId);
        }

        public static LibraryCardAuthResult failure(String message) {
            return new LibraryCardAuthResult(false, message, null, null);
        }

        public static LibraryCardAuthResult failureWithDetails(String message, String extractedName, String extractedStudentId) {
            return new LibraryCardAuthResult(false, message, extractedName, extractedStudentId);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getExtractedName() {
            return extractedName;
        }

        public String getExtractedStudentId() {
            return extractedStudentId;
        }
    }
}

