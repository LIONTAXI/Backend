package taxi.tago.dto.Admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminLoginRequest {
    private String email; // 관리자 이메일 (아이디)
    private String password; // 비밀번호
}

