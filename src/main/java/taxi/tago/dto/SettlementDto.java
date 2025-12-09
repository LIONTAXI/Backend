package taxi.tago.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// 정산 관련 DTO 묶음
// CreateRequest : 정산 생성 요청
// ParticipantShare : 정산 생성 시 각 참여자별 금액 정보
// DetailResponse : 정산 상세 조회 응답
// ParticipantResponse : 상세 조회에서 보여줄 각 참여자 정보
public class SettlementDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        // Settlement PK
        private Long settlementId; // 정산 ID

        // 어떤 택시팟에 대한 정산인지
        private Long taxiPartyId;

        // 택시 총 요금
        private Integer totalFare;

        // 정산에 사용할 은행, 계좌 정보
        private String bankName;
        private String accountNumber;

        // 참여자별 금액 정보 목록 (프론트에서 1/N 자동 계산 후 사용자가 수정 가능 → 서버에는 최종 확정 금액만 전달)
        private List<ParticipantShare> participants;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantShare {
        private Long userId; // 정산 대상 사용자 ID
        private Integer amount; // 이 사용자가 부담할 금액
    }

    @Getter
    @AllArgsConstructor
    public static class DetailResponse {
        private Long settlementId; // 정산 ID
        private Long taxiPartyId; // 연결된 택시팟 ID
        private Integer totalFare; // 택시 총 요금

        // 계좌 정보
        private String bankName;
        private String accountNumber;

        private String status; // IN_PROGRESS / COMPLETED
        private LocalDateTime createdAt; // 정산 생성 시각
        private List<ParticipantResponse> participants; // 참여자 목록
    }

    @Getter
    @AllArgsConstructor
    public static class ParticipantResponse {
        private Long userId;
        private String name;
        private String shortStudentId;
        private String imgUrl;
        private Integer amount;
        private boolean paid;
        private LocalDateTime paidAt;
        private boolean host; // 총대인지 여부
    }
}
