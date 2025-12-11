package taxi.tago.service.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.UserRole;
import taxi.tago.dto.MypageDto;
import taxi.tago.entity.User;
import taxi.tago.repository.UserRepository;
import taxi.tago.service.EmailAuthService;
import taxi.tago.service.FileStorageService;
import taxi.tago.util.PasswordValidator;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    // 기본 프로필 이미지 경로
    private static final String DEFAULT_PROFILE_IMAGE = "/images/default.png";

    private final UserRepository userRepository;
    private final EmailAuthService emailAuthService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    // 회원가입 처리
    @Transactional
    public User register(String email, String password, String confirmPassword, String studentId, String name) {
        // 1. 이메일 인증 완료 여부 확인
        if (!emailAuthService.isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다. 먼저 이메일 인증을 완료해주세요.");
        }

        // 2. 이미 가입된 이메일인지 확인
        // 주의: 환경변수로 관리되는 관리자 계정은 체크하지 않음 (관리자 계정과 일반 사용자 계정은 같은 이메일로 공존 가능)
        // DB에 저장된 USER role의 일반 사용자 계정만 체크 (ADMIN role은 제외)
        if (userRepository.findByEmailAndRole(email, UserRole.USER).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 3. 비밀번호 유효성 검증
        String passwordError = PasswordValidator.validate(password);
        if (passwordError != null) {
            throw new IllegalArgumentException(passwordError);
        }

        // 4. 비밀번호 일치 확인
        if (!PasswordValidator.matches(password, confirmPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 5. 비밀번호 암호화 및 사용자 생성
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        // 학번과 이름은 도서관 인증에서 나중에 설정 (null 허용)
        if (studentId != null && !studentId.trim().isEmpty()) {
            user.setStudentId(studentId.trim());
        }
        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        user.setLastActiveAt(LocalDateTime.now());
        user.setImgUrl(DEFAULT_PROFILE_IMAGE);

        // 7. 사용자 저장 (PrePersist에서 shortStudentId 자동 추출됨)
        User savedUser = userRepository.save(user);

        // 8. 인증 완료 상태 제거 (이미 사용됨)
        emailAuthService.removeVerifiedEmail(email);

        log.info("회원가입 완료: {}, 학번: {}, 이름: {}", email, 
                studentId != null ? studentId : "미설정", 
                name != null ? name : "미설정");
        return savedUser;
    }

    // 완전한 회원가입 처리 (도서관 인증 완료 후 호출)
    @Transactional
    public User completeRegistration(String email, String password, String studentId, String name) {
        // 1. 이메일 인증 완료 여부 확인
        if (!emailAuthService.isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 이미 가입된 이메일인지 확인
        if (userRepository.findByEmailAndRole(email, UserRole.USER).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 3. 비밀번호 유효성 검증
        String passwordError = PasswordValidator.validate(password);
        if (passwordError != null) {
            throw new IllegalArgumentException(passwordError);
        }

        // 4. 학번과 이름 필수 확인
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("학번이 필요합니다.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름이 필요합니다.");
        }

        // 5. 비밀번호 암호화 및 사용자 생성
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setStudentId(studentId.trim());
        user.setName(name.trim());
        user.setLastActiveAt(LocalDateTime.now());
        user.setImgUrl(DEFAULT_PROFILE_IMAGE);

        // 6. 사용자 저장 (PrePersist에서 shortStudentId 자동 추출됨)
        User savedUser = userRepository.save(user);

        // 7. 인증 완료 상태 제거 (이미 사용됨)
        emailAuthService.removeVerifiedEmail(email);

        log.info("완전한 회원가입 완료: {}, 학번: {}, 이름: {}", email, studentId, name);
        return savedUser;
    }


    @Transactional
    public User login(String email, String password) {
        // 1. 이메일로 사용자 조회 (USER, ADMIN 모두 가능)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호를 다시 확인해주세요."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 다시 확인해주세요.");
        }

        // 3. 마지막 활동 시간 업데이트
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("사용자 로그인 성공!: {}", email);
        return user;
    }

    
    public void sendPasswordResetCode(String email) {
        // 1. 사용자 존재 여부 확인
        if (!userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 인증코드 발송
        emailAuthService.sendPasswordResetCode(email);

        log.info("비밀번호 변경용 인증코드 발송 완료: {}", email);
    }


    public boolean verifyPasswordResetCode(String email, String code) {
        // 1. 사용자 존재 여부 확인
        if (!userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 인증코드 검증
        boolean isValid = emailAuthService.verifyPasswordResetCode(email, code);

        if (isValid) {
            log.info("비밀번호 변경용 인증코드 검증 성공: {}", email);
        } else {
            log.warn("비밀번호 변경용 인증코드 검증 실패: {}", email);
        }

        return isValid;
    }

    // 비밀번호 변경을 위한 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
    public void resendPasswordResetCode(String email) {
        // 1. 사용자 존재 여부 확인
        if (!userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
        emailAuthService.resendPasswordResetCode(email);

        log.info("비밀번호 변경용 인증코드 재전송 완료: {}", email);
    }

    // 비밀번호 변경 처리
    @Transactional
    public void changePassword(String email, String newPassword, String confirmPassword) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 비밀번호 변경용 인증 완료 여부 확인
        if (!emailAuthService.isPasswordResetVerified(email)) {
            throw new IllegalArgumentException("인증이 완료되지 않았습니다. 먼저 이메일 인증을 완료해주세요.");
        }

        // 3. 비밀번호 유효성 검증
        String passwordError = PasswordValidator.validate(newPassword);
        if (passwordError != null) {
            throw new IllegalArgumentException(passwordError);
        }

        // 4. 비밀번호 일치 확인
        if (!PasswordValidator.matches(newPassword, confirmPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 5. 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        // 6. 인증 완료 상태 제거 (이미 사용됨)
        emailAuthService.removePasswordResetVerifiedEmail(email);

        log.info("비밀번호 변경 완료: {}", email);
    }

    // 마이페이지_프로필수정_기존정보조회
    @Transactional(readOnly = true)
    public MypageDto.InfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. id=" + userId));

        // 항상 API 엔드포인트를 반환 (기본 이미지든 업로드된 이미지든 모두 API를 통해 제공)
        // getProfileImage API에서 imgUrl을 확인하여 적절한 이미지를 반환함
        String imgUrl = "/api/users/" + userId + "/profile-image";

        return new MypageDto.InfoResponse(
                imgUrl,
                user.getName(),
                user.getShortStudentId(),
                user.getEmail()
        );
    }

    // 마이페이지_프로필수정_프로필사진업로드
    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. id=" + userId));

        // 1. 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
        }

        // 2. 파일명 가져오기 및 확장자 유무 확인
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 형식이 올바르지 않습니다.");
        }

        // 3. 확장자 추출 및 검사 (보안 로직: jpg, png 등만 허용)
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 가능)");
        }

        // 4. FileStorageService를 사용하여 파일 저장 (프로덕션 환경에서도 동작)
        String savedFilePath = fileStorageService.saveImageFile(file);

        // 5. DB에 저장할 경로 (API 엔드포인트로 접근 가능하도록)
        String dbImgUrl = "/api/users/" + userId + "/profile-image";

        // 6. 엔티티 업데이트 (파일 경로를 imgUrl에 저장)
        // imgUrl에 실제 파일 경로를 저장하여 나중에 API에서 읽을 수 있도록 함
        user.setImgUrl(savedFilePath);

        return "프로필 사진 수정 완료, URL: " + dbImgUrl;
    }
}