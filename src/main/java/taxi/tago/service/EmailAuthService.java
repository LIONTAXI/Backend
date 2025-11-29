package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final JavaMailSender mailSender;

    @Value("${email.auth.code.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${email.auth.code.length:6}")
    private int codeLength;

    // 이메일별 인증 코드 저장 (실제 운영 환경에서는 Redis 등을 사용 권장)
    private final Map<String, AuthCodeInfo> authCodeStorage = new ConcurrentHashMap<>();

    // 인증 완료된 이메일 추적 (비밀번호 설정 단계를 위해)
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    // 서울여대 웹메일 도메인 검증
    private static final String SWU_EMAIL_DOMAIN = "@swu.ac.kr";

    // 인증 코드 생성 및 이메일 전송
    public void sendAuthCode(String email) {
        // 서울여대 웹메일 검증
        if (!isSwuEmail(email)) {
            throw new IllegalArgumentException("서울여대 웹메일(@swu.ac.kr)만 사용 가능합니다.");
        }

        // 6자리 인증 코드 생성
        String authCode = generateAuthCode();

        // 기존 코드가 있으면 초기화 (재전송 시)
        authCodeStorage.put(email, new AuthCodeInfo(authCode, LocalDateTime.now()));

        // 이메일 전송
        sendEmail(email, authCode);

        log.info("인증 코드 전송 완료: {}", email);
    }

    // 인증 코드 검증
    public boolean verifyAuthCode(String email, String code) {
        AuthCodeInfo authCodeInfo = authCodeStorage.get(email);

        if (authCodeInfo == null) {
            log.warn("인증 코드가 존재하지 않음: {}", email);
            return false;
        }

        // 만료 시간 확인
        if (authCodeInfo.isExpired(expirationMinutes)) {
            log.warn("인증 코드 만료: {}", email);
            authCodeStorage.remove(email);
            return false;
        }

        // 코드 일치 확인
        boolean isValid = authCodeInfo.getCode().equals(code);
        if (isValid) {
            // 인증 성공 시 저장소에서 제거하고 인증 완료 상태 저장
            authCodeStorage.remove(email);
            verifiedEmails.put(email, true);
            log.info("인증 코드 검증 성공: {}", email);
        } else {
            log.warn("인증 코드 불일치: {}", email);
        }

        return isValid;
    }

    // 인증 코드 재전송 (기존 코드 초기화 후 새 코드 전송)
    public void resendAuthCode(String email) {
        // 기존 코드 제거 (초기화)
        authCodeStorage.remove(email);
        
        // 새 코드 생성 및 전송
        sendAuthCode(email);
        log.info("인증 코드 재전송 완료: {}", email);
    }

    // 이메일 인증 완료 여부 확인
    public boolean isEmailVerified(String email) {
        return verifiedEmails.getOrDefault(email, false);
    }

    // 이메일 인증 상태 제거 (회원가입 완료 후 또는 만료 시)
    public void removeVerifiedEmail(String email) {
        verifiedEmails.remove(email);
    }

    // 서울여대 웹메일인지 확인
    private boolean isSwuEmail(String email) {
        return email != null && email.toLowerCase().endsWith(SWU_EMAIL_DOMAIN);
    }

    // 6자리 랜덤 인증 코드 생성
    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // 이메일 전송
    private void sendEmail(String to, String authCode) {
        try {
            // 로컬 테스트용: 콘솔에 인증 코드 출력
            log.info("========================================");
            log.info("이메일 인증 코드 전송");
            log.info("받는 사람: {}", to);
            log.info("인증 코드: {}", authCode);
            log.info("유효 시간: {}분", expirationMinutes);
            log.info("========================================");
            
            // 실제 이메일 전송 (Gmail 설정이 되어 있을 경우)
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[타고] 회원가입 인증 코드");
            message.setText("안녕하세요.\n\n" +
                    "회원가입을 위한 인증 코드입니다.\n\n" +
                    "인증 코드: " + authCode + "\n\n" +
                    "이 코드는 " + expirationMinutes + "분간 유효합니다.\n\n" +
                    "본인이 요청한 것이 아니라면 무시하셔도 됩니다.");
            mailSender.send(message);
            log.info("이메일 전송 성공: {}", to);
        } catch (Exception e) {
            // 이메일 전송 실패해도 로그에 출력했으므로 계속 진행
            log.warn("이메일 전송 실패 (로컬 테스트 모드): {} - 인증 코드는 위 로그에서 확인하세요", to);
            log.warn("실제 이메일 전송을 원하시면 Gmail 앱 비밀번호를 설정하세요", e);
        }
    }

    // 인증 코드 정보를 저장하는 내부 클래스
    private static class AuthCodeInfo {
        private final String code;
        private final LocalDateTime createdAt;

        public AuthCodeInfo(String code, LocalDateTime createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }

        public String getCode() {
            return code;
        }

        public boolean isExpired(int expirationMinutes) {
            return LocalDateTime.now().isAfter(createdAt.plusMinutes(expirationMinutes));
        }
    }
}

