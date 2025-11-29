package taxi.tago.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import taxi.tago.constant.UserRole;
import taxi.tago.entity.User;
import taxi.tago.repository.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // 관리자 계정이 이미 존재하는지 확인
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            // 관리자 계정 생성
            String encodedPassword = passwordEncoder.encode(adminPassword);
            
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(encodedPassword);
            admin.setRole(UserRole.ADMIN);
            admin.setLastActiveAt(LocalDateTime.now());
            
            userRepository.save(admin);
            log.info("관리자 계정이 생성되었습니다: {}", adminEmail);
        } else {
            log.info("관리자 계정이 이미 존재합니다: {}", adminEmail);
        }
    }
}

