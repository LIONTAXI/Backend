package taxi.tago.util;

import java.util.regex.Pattern;

public class PasswordValidator {
    
    // 최소 8자리 이상
    private static final int MIN_LENGTH = 8;
    
    // 허용된 특수문자: !@#$%^&*
    private static final String SPECIAL_CHARS = "!@#$%^&*";
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[" + Pattern.quote(SPECIAL_CHARS) + "].*");
    
    /**
     * 비밀번호 유효성 검증
     * @param password 비밀번호
     * @return 검증 결과 메시지 (null이면 유효함)
     */
    public static String validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "비밀번호를 입력해주세요.";
        }
        
        // 8자리 이상 체크
        if (password.length() < MIN_LENGTH) {
            return "비밀번호는 8자리 이상이어야 합니다.";
        }
        
        // 특수문자 체크
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            return "비밀번호에 특수문자(!@#$%^&*)를 포함해주세요.";
        }
        
        return null; // 유효함
    }
    
    /**
     * 비밀번호 일치 여부 확인
     * @param password 비밀번호
     * @param confirmPassword 비밀번호 확인
     * @return 일치 여부
     */
    public static boolean matches(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
}
