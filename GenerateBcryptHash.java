import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBcryptHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "test123!";
        
        // 5개의 해시 생성 (모두 같은 비밀번호지만 해시는 다름)
        System.out.println("Password: " + password);
        System.out.println("\nBCrypt Hashes (모두 같은 비밀번호로 로그인 가능):");
        for (int i = 1; i <= 5; i++) {
            String hash = encoder.encode(password);
            System.out.println("Hash " + i + ": " + hash);
        }
        
        // 검증 테스트
        String testHash = encoder.encode(password);
        System.out.println("\n검증 테스트:");
        System.out.println("Password 'test123!' matches hash: " + encoder.matches(password, testHash));
    }
}

