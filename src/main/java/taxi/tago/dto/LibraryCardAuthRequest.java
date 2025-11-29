package taxi.tago.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LibraryCardAuthRequest {
    private String userId; // 사용자 ID 또는 이메일
    private String name; // 사용자 이름 (검증용)
    private String studentId; // 학번 (검증용)
}

