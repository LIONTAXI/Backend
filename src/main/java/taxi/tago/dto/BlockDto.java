package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class BlockDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockRequest {
        private Long blockerId; // 나 (차단하는 사람)
        private Long blockedId; // 상대방 (차단당하는 사람)
    }
}