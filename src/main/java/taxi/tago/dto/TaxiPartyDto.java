package taxi.tago.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

public class TaxiPartyDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @Schema(description = "총대슈니 ID", example = "1")
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
}