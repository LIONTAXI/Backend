package taxi.tago.dto.Email;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailAuthRequest {
    private String email;
    private String code; // 인증 코드 검증 시 사용
}

