package taxi.tago.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

public class TaxiPartyDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateRequest {
        @Schema(hidden = true)
        private Long userId;

        @Schema(description = "승차지", example = "화랑대역 2번출구")
        private String departure;

        @Schema(description = "하차지", example = "서울여대 50주년기념관")
        private String destination;

        @Schema(description = "모집 마감 시각", example = "14:50")
        @JsonFormat(pattern = "HH:mm")
        private LocalTime meetingTime;

        @Schema(description = "모집 인원", example = "4")
        private Integer maxParticipants;

        @Schema(description = "예상 가격", example = "5000")
        private Integer expectedPrice;

        @Schema(description = "추가 설명")
        private String content;
    }

    @Getter
    @AllArgsConstructor
    public static class InfoResponse {
        private Long id;                 // 클릭 시 이동을 위한 ID
        private String departure;        // 승차지
        private String destination;      // 하차지

        @JsonFormat(pattern = "HH:mm")   // "14:30" 형식
        private LocalTime meetingTime;   // 마감 시각

        private Integer currentParticipants; // 현재 인원
        private Integer maxParticipants;     // 모집 인원
        private Integer expectedPrice;       // 예상 가격
    }

    // 택시팟 정보
    @Getter
    @AllArgsConstructor
    public static class DetailResponse {
        private Long id;                 // 택시팟 ID
        private Long hostId;             // 총대슈니 ID
        private String departure;        // 승차지
        private String destination;      // 하차지

        @JsonFormat(pattern = "HH:mm")
        private LocalTime meetingTime;   // 마감 시각

        private Integer currentParticipants; // 현재 인원
        private Integer maxParticipants;     // 모집 인원
        private Integer expectedPrice;       // 예상 가격
        private String content;              // 추가 설명
        private String status;               // 매칭 상태
        private String participationStatus;  // 나의 참여상태(NONE, WAITING, ACCEPTED)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequest {
        private Long userId;
        private String departure;       // 승차지
        private String destination;     // 하차지

        @JsonFormat(pattern = "HH:mm")
        private LocalTime meetingTime;  // 마감 시각

        private Integer maxParticipants;// 모집 인원
        private Integer expectedPrice;  // 예상 가격
        private String content;         // 추가 설명
    }
}