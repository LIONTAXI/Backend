package taxi.tago.dto.Email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailAuthResponse {
    private boolean success;
    private String message;
    private String email; // 응답에 이메일 포함 (선택사항)
}

