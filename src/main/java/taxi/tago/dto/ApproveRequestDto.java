package taxi.tago.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApproveRequestDto {
    private Long authId;
    private Boolean isApproved; // true: 승인, false: 반려
    private String rejectionReason; // 반려 사유 (반려 시 필수, 3가지 중 하나: "이미지와 입력 정보 불일치", "이미지 정보 미포함", "이미지 부정확")
}

