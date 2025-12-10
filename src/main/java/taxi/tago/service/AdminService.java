package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import taxi.tago.constant.UserRole;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    // 관리자 로그인 처리 (환경변수 기반 인증)
    public AdminInfo login(String email, String password) {
        // 1. 관리자 이메일 확인 (환경변수와 비교)
        if (!adminEmail.equals(email)) {
            throw new IllegalArgumentException("존재하지 않는 계정입니다.");
        }

        // 2. 비밀번호 확인 (환경변수와 비교)
        if (!adminPassword.equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        log.info("관리자 로그인 성공: {}", email);
        
        // 관리자 정보 반환 (DB에 저장하지 않음)
        return new AdminInfo(email, UserRole.ADMIN);
    }

    // 관리자 정보를 담는 내부 클래스
    public static class AdminInfo {
        private final String email;
        private final UserRole role;

        public AdminInfo(String email, UserRole role) {
            this.email = email;
            this.role = role;
        }

        public String getEmail() {
            return email;
        }

        public UserRole getRole() {
            return role;
        }
    }
}

