package taxi.tago.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class UserMapDto {

    // 유저 위치 및 마지막 활동 시간 업데이트
    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateRequest {
        @Schema(hidden = true)
        private Long userId;
        @Schema(description = "위도", example = "37.619812")
        private Double latitude;
        @Schema(description = "경도", example = "127.077598")
        private Double longitude;
    }

    // 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내)
    @Data
    @AllArgsConstructor
    public static class Response {
        private Long userId;
        private Double latitude;
        private Double longitude;
        private String markerEmoji;

        // 생성자 메서드 (이모지 자동 👤 설정)
        public static Response of(Long userId, Double lat, Double lon, String emoji) {
            return new Response(
                    userId,
                    lat,
                    lon,
                    emoji
            );
        }
    }
}