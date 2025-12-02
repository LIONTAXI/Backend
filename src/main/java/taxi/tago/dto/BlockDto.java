package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class BlockDto {

    // 차단하기
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockRequest {
        private Long blockerId; // 나 (차단하는 사람)
        private Long blockedId; // 상대방 (차단당하는 사람)
    }

    // 내가 차단한 목록
    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long blockId;       // 차단 내역 ID
        private Long blockedUserId; // 차단된 사람의 유저 ID
        private String blockedUserName; // 차단된 사람 이름
        private String blockedUserImgUrl;   // 차단된 사람 프로필 사진
        private String blockedUserShortStudentId; // 차단된 사람 학번 (2자리)
    }
}