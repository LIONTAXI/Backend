package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import taxi.tago.constant.ParticipationStatus;

public class TaxiUserDto {
    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 조회&수락
    @Getter
    @AllArgsConstructor
    public static class RequestResponse {
        private Long taxiUserId; // 신청 내역 ID
        private Long userId;     // 신청자 유저 ID
        private String name;          // 이름
        private String shortStudentId; // 학번 (2자리, 예: "23")
        private String imgUrl;        // 프로필 사진 URL
        private ParticipationStatus status; // 요청 상태, WAITING 또는 ACCEPTED
    }
}