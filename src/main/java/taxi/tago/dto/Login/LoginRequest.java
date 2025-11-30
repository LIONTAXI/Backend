package taxi.tago.dto.Login;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {
    private String email; // 웹메일 (아이디)
    private String password; // 비밀번호
}

