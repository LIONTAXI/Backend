package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import taxi.tago.constant.AuthStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String extractedName;
    private String extractedStudentId;
    private String imagePath;
    private String imageUrl; // 이미지 조회 URL
    private AuthStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String failureReason;
}

