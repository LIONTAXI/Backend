package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import taxi.tago.entity.LibraryCardAuth;
import taxi.tago.entity.User;
import taxi.tago.repository.LibraryCardAuthRepository;
import taxi.tago.repository.UserRepository;
import taxi.tago.service.NaverOcrService.OcrResult;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryCardAuthService {

    private final NaverOcrService ocrService;
    private final UserRepository userRepository;
    private final LibraryCardAuthRepository libraryCardAuthRepository;

    // 서울여대 학번 패턴: 10자리 숫자 (예: 2021111222)
    private static final Pattern SWU_STUDENT_ID_PATTERN = Pattern.compile("^\\d{10}$");

    /**
     * 도서관 전자출입증 이미지를 OCR로 처리 (userId 없이 이미지만 등록)
     * @param imageFile 이미지 파일
     * @return OCR 인식 결과
     */
    public LibraryCardAuthResult processLibraryCardImage(MultipartFile imageFile) {
        try {
            // 1. 이미지 파일 검증
            if (imageFile == null || imageFile.isEmpty()) {
                return LibraryCardAuthResult.failure("이미지를 등록해주세요.");
            }

            // 2. OCR로 이미지에서 텍스트 추출
            byte[] imageBytes = imageFile.getBytes();
            OcrResult ocrResult = ocrService.extractText(imageBytes);

            String extractedName = ocrResult.getName();
            String extractedStudentId = ocrResult.getStudentId();

            // 3. 이름 또는 학번이 추출되지 않은 경우
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

            // 4. 서울여대 학번 형식 검증
            if (!isValidSwuStudentId(extractedStudentId)) {
                return LibraryCardAuthResult.failureWithDetails(
                    "서울여대 학번 형식이 아닙니다. 올바른 전자출입증을 등록해주세요.",
                    extractedName, extractedStudentId
                );
            }

            // 5. OCR 인식 성공
            log.info("도서관 전자출입증 OCR 인식 성공: name={}, studentId={}", extractedName, extractedStudentId);

            return LibraryCardAuthResult.success(
                "인증 완료!",
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

    /**
     * 도서관 전자출입증 이미지를 업로드하고 인증 처리 (기존 메서드 - 하위 호환성 유지)
     * @param userId 사용자 ID (이메일 또는 ID)
     * @param imageFile 이미지 파일
     * @return 인증 결과
     */
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

    //인증 기록 생성
    private LibraryCardAuth createAuthRecord(User user, String extractedName, String extractedStudentId, 
                                            boolean isSuccess, String failureReason) {
        LibraryCardAuth auth = new LibraryCardAuth();
        auth.setUser(user);
        auth.setExtractedName(extractedName);
        auth.setExtractedStudentId(extractedStudentId);
        auth.setIsSuccess(isSuccess);
        auth.setFailureReason(failureReason);
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

