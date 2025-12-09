package taxi.tago.service.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.dto.MypageDto;
import taxi.tago.entity.User;
import taxi.tago.repository.UserRepository;
import taxi.tago.service.EmailAuthService;
import taxi.tago.util.PasswordValidator;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailAuthService emailAuthService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리
     *
     * @param email           웹메일 (아이디)
     * @param password        비밀번호
     * @param confirmPassword 비밀번호 확인
     * @param studentId       학번 (도서관 전자출입증 인증으로 추출된 학번)
     * @param name            이름 (도서관 전자출입증 인증으로 추출된 이름)
     * @return 생성된 User 객체
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    @Transactional
    public User register(String email, String password, String confirmPassword, String studentId, String name) {
        // 1. 이메일 인증 완료 여부 확인
        if (!emailAuthService.isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다. 먼저 이메일 인증을 완료해주세요.");
        }

        // 2. 이미 가입된 이메일인지 확인
        if (userRepository.findByEmail(email).isPresent()) {
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

        // 5. 도서관 전자출입증 인증 필수 확인
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("도서관 전자출입증 인증이 완료되지 않았습니다. 전자출입증을 등록해주세요.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("도서관 전자출입증 인증이 완료되지 않았습니다. 전자출입증을 등록해주세요.");
        }

        // 6. 비밀번호 암호화 및 사용자 생성
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setStudentId(studentId.trim());
        user.setName(name.trim());
        user.setLastActiveAt(LocalDateTime.now());
        user.setImgUrl("/images/default.svg");

        // 7. 사용자 저장 (PrePersist에서 shortStudentId 자동 추출됨)
        User savedUser = userRepository.save(user);

        // 8. 인증 완료 상태 제거 (이미 사용됨)
        emailAuthService.removeVerifiedEmail(email);

        log.info("회원가입 완료: {}, 학번: {}, 이름: {}", email, studentId, name);
        return savedUser;
    }

    /**
     * 사용자 로그인 처리
     *
     * @param email    웹메일 (아이디)
     * @param password 비밀번호
     * @return 로그인 성공 시 User 객체
     * @throws IllegalArgumentException 로그인 실패 시
     */
    @Transactional
    public User login(String email, String password) {
        // 1. 이메일로 사용자 조회
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

    /**
     * 비밀번호 변경을 위한 인증코드 발송
     *
     * @param email 로그인된 사용자의 웹메일 주소
     * @throws IllegalArgumentException 사용자가 존재하지 않을 경우
     */
    public void sendPasswordResetCode(String email) {
        // 1. 사용자 존재 여부 확인
        if (!userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 인증코드 발송
        emailAuthService.sendPasswordResetCode(email);

        log.info("비밀번호 변경용 인증코드 발송 완료: {}", email);
    }

    /**
     * 비밀번호 변경을 위한 인증코드 검증
     *
     * @param email 로그인된 사용자의 웹메일 주소
     * @param code  입력한 인증코드
     * @return 인증코드 일치 여부 (true: 일치, false: 불일치)
     */
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

    /**
     * 비밀번호 변경을 위한 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
     *
     * @param email 로그인된 사용자의 웹메일 주소
     * @throws IllegalArgumentException 사용자가 존재하지 않을 경우
     */
    public void resendPasswordResetCode(String email) {
        // 1. 사용자 존재 여부 확인
        if (!userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 2. 인증코드 재전송 (기존 코드 초기화 후 새 코드 전송)
        emailAuthService.resendPasswordResetCode(email);

        log.info("비밀번호 변경용 인증코드 재전송 완료: {}", email);
    }

    /**
     * 비밀번호 변경 처리
     *
     * @param email           로그인된 사용자의 웹메일 주소
     * @param newPassword     새로운 비밀번호
     * @param confirmPassword 비밀번호 확인
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
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

        return new MypageDto.InfoResponse(
                user.getImgUrl(),
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

        // 2. 파일명 가져오기 및 확장자 유무 확인 (안전장치 추가)
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

        // 4. 파일 이름 중복 방지를 위한 UUID 생성 (검사 통과 후에 만듦)
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + "_" + originalFilename;

        // 5. 파일 저장 경로 설정
        String projectPath = System.getProperty("user.dir")
                + File.separator + "src"
                + File.separator + "main"
                + File.separator + "resources"
                + File.separator + "static"
                + File.separator + "images";

        // 6. 실제 파일 저장 (폴더가 없으면 자동 생성)
        File saveFile = new File(projectPath, fileName);
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }

        file.transferTo(saveFile);

        // 7. DB에 저장할 URL 만들기
        String dbImgUrl = "/images/" + fileName;

        // 8. 엔티티 업데이트 (Dirty Checking)
        user.setImgUrl(dbImgUrl);

        return "프로필 사진 수정 완료, 경로: " + dbImgUrl;
    }
}