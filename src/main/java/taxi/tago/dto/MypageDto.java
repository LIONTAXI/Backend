package taxi.tago.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MypageDto {

    // 마이페이지_프로필수정_기존정보조회
    @Getter
    @AllArgsConstructor
    @Schema(description = "마이페이지 정보 응답")
    public static class InfoResponse {
        @Schema(description = "프로필 사진 URL", example = "/images/default_profile.png")
        private String imgUrl;

        @Schema(description = "이름", example = "김슈니")
        private String name;

        @Schema(description = "학번(2자리)", example = "23")
        private String shortStudentId;

        @Schema(description = "이메일", example = "swu@swu.ac.kr")
        private String email;
    }
}