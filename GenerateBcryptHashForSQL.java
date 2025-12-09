import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class GenerateBcryptHashForSQL {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "test123!";
        
        // 하나의 해시 생성 (모든 계정에 동일하게 사용)
        String hash = encoder.encode(password);
        
        System.out.println("============================================");
        System.out.println("BCrypt 해시 생성 완료");
        System.out.println("============================================");
        System.out.println("비밀번호: " + password);
        System.out.println("BCrypt 해시: " + hash);
        System.out.println();
        System.out.println("============================================");
        System.out.println("SQL INSERT 문 (복사해서 사용하세요)");
        System.out.println("============================================");
        System.out.println();
        
        // SQL 생성
        System.out.println("DELETE FROM users WHERE email LIKE 'test%@swu.ac.kr';");
        System.out.println();
        System.out.println("INSERT INTO users (email, password, role, img_url, last_active_at) VALUES");
        for (int i = 1; i <= 5; i++) {
            String comma = (i < 5) ? "," : ";";
            System.out.println("('test" + i + "@swu.ac.kr', '" + hash + "', 'USER', '/images/default.svg', NOW())" + comma);
        }
        System.out.println();
        System.out.println("-- 로그인 정보:");
        System.out.println("-- 이메일: test1@swu.ac.kr ~ test5@swu.ac.kr");
        System.out.println("-- 비밀번호: " + password);
    }
}

