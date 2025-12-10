package taxi.tago.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Value("${admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) throws Exception {
        // 관리자 계정은 환경변수로만 관리 (DB에 저장하지 않음)
        log.info("관리자 계정은 환경변수로 관리됩니다: {}", adminEmail);
    }
}

