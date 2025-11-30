package taxi.tago.dto.Password;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordSetRequest {
    private String email; // 웹메일 (아이디)
    private String password; // 비밀번호
    private String confirmPassword; // 비밀번호 확인
}
