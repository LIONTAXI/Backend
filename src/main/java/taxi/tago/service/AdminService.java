package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import taxi.tago.constant.UserRole;
import taxi.tago.entity.User;
import taxi.tago.repository.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 관리자 로그인 처리
     * @param email 관리자 이메일
     * @param password 비밀번호
     * @return 로그인 성공 시 User 객체, 실패 시 null
     */
    public User login(String email, String password) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        // 2. 관리자 권한 확인
        if (user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자 권한이 없습니다.");
        }

        // 3. 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 4. 마지막 활동 시간 업데이트
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("관리자 로그인 성공: {}", email);
        return user;
    }
}

