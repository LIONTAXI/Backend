package taxi.tago.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApproveRequestDto {
    private Long authId;
    private String reason; // 거부 사유 (거부 시에만 사용)
}

