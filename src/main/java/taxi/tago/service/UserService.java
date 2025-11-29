package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.entity.User;
import taxi.tago.repository.UserRepository;
import taxi.tago.util.PasswordValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailAuthService emailAuthService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리
     * @param email 웹메일 (아이디)
     * @param password 비밀번호
     * @param confirmPassword 비밀번호 확인
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    @Transactional
    public void register(String email, String password, String confirmPassword) {
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

        // 5. 비밀번호 암호화 및 사용자 생성
        String encodedPassword = passwordEncoder.encode(password);
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setLastActiveAt(LocalDateTime.now());

        // 6. 사용자 저장
        userRepository.save(user);
        
        // 7. 인증 완료 상태 제거 (이미 사용됨)
        emailAuthService.removeVerifiedEmail(email);
        
        log.info("회원가입 완료: {}", email);
    }

    /**
     * 사용자 로그인 처리
     * @param email 웹메일 (아이디)
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

        log.info("사용자 로그인 성공: {}", email);
        return user;
    }

    /**
     * 비밀번호 변경을 위한 인증코드 발송
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
     * @param email 로그인된 사용자의 웹메일 주소
     * @param code 입력한 인증코드
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
}
