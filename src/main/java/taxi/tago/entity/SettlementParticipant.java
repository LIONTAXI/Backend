package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

// 정산 참여자 엔티티
@Entity
@Table(name = "settlement_participants")
@Getter
@Setter // paid, paidAt 업데이트 때문에 Setter 허용
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_participant_id")
    private Long id;

    // 어떤 정산에 속해 있는지
    // SettlementParticipant(N) : Settlement(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    // 정산 대상 사용자
    //SettlementParticipant(N) : User(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 이 사용자가 부담해야 할 금액
    @Column(name = "amount", nullable = false)
    private Integer amount;

    // 납부 완료 여부
    @Column(name = "is_paid", nullable = false)
    private boolean paid = false;

    // 납부 완료 시각 (미완료 시 null)
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 총대인지 여부
    @Column(name = "is_host", nullable = false)
    private boolean host;

    // 생성자
    public SettlementParticipant(User user, Integer amount, boolean host) {
        if (user == null) {
            throw new IllegalArgumentException("정산 대상자는 null일 수 없습니다.");
        }
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("정산 금액은 0원 이상이어야 합니다.");
        }

        this.user = user;
        this.amount = amount;
        this.host = host;
    }

    // 납부 완료 처리 메서드 (이미 paid == true인 경우에는 중복처리를 막기 위해 아무 처리도 하지 않음)
    public void markPaid() {
        if (!this.paid) {
            this.paid = true;
            this.paidAt = LocalDateTime.now();
        }
    }
}
