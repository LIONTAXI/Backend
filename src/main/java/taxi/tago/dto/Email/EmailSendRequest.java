package taxi.tago.dto.Email;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailSendRequest {
    private String email; // 인증 코드를 받을 이메일 주소
}

