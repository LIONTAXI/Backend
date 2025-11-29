package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LibraryCardAuthResponse {
    private boolean success; // 인증 성공 여부
    private String message; // 응답 메시지
    private String extractedName; // OCR로 추출한 이름
    private String extractedStudentId; // OCR로 추출한 학번
}

