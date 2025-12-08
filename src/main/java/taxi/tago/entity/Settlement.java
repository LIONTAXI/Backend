package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import taxi.tago.constant.SettlementStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 택시 정산 엔티티
@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    // 어떤 택시팟에 대한 정산인지
    // TaxiParty와 1:1 관계 (하나의 택시팟에는 최대 하나의 정산 정보만 존재)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxiparty_id", nullable = false, unique = true)
    private TaxiParty taxiParty;

    // 정산 총대슈니
    // Settlement(N) : User(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // 총 택시비
    @Column(name = "total_fare", nullable = false)
    private Integer totalFare;

    // 정산에 사용되는 계좌 정보
    // - bankName: 은행 이름
    // - accountNumber: 계좌번호
    @Column(name = "bank_name", length = 30, nullable = false)
    private String bankName;

    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber;

    // 정산 상태 (IN_PROGRESS / COMPLETED)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.IN_PROGRESS;

    // 정산 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막으로 정산 재촉을 보낸 시각 (2시간에 1번만 재촉 가능하도록 제한하기 위한 필드) (아직 재촉한 적 없으면 null)
    @Column(name = "last_reminded_at")
    private LocalDateTime lastRemindedAt;

    // 참여자별 정산 내역 (1:N)
    // Settlement(1) : SettlementParticipant(N)
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementParticipant> participants = new ArrayList<>();

    // 정산 생성용 정적 팩토리 메서드
    public static Settlement create(
            TaxiParty taxiParty,
            User host,
            Integer totalFare,
            String bankName,
            String accountNumber
    ) {
        if (taxiParty == null) {
            throw new IllegalArgumentException("정산 생성 시 TaxiParty는 필수입니다.");
        }
        if (host == null) {
            throw new IllegalArgumentException("정산 생성 시 host는 필수입니다.");
        }
        if (totalFare == null || totalFare <= 0) {
            throw new IllegalArgumentException("총 금액은 0원보다 커야 합니다.");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("은행명은 필수입니다.");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다.");
        }

        Settlement settlement = new Settlement();
        settlement.taxiParty = taxiParty;
        settlement.host = host;
        settlement.totalFare = totalFare;
        settlement.bankName = bankName;
        settlement.accountNumber = accountNumber;
        settlement.status = SettlementStatus.IN_PROGRESS;
        return settlement;
    }

    // 연관관계 편의 메서드
    // SettlementParticipant 를 이 Settlement에 추가할 때 항상 이 메서드를 사용해서 양방향 연관관계의 일관성을 보장함
    public void addParticipant(SettlementParticipant participant) {
        participants.add(participant); // 컬렉션에 추가
        participant.setSettlement(this); // 반대편 연관관계도 함께 세팅
    }

    // 모든 참여자가 납부를 완료했는지 검사한 뒤 상태를 COMPLETED로 변경
    public void updateStatusIfCompleted() {
        boolean allPaid = participants.stream()
                .allMatch(SettlementParticipant::isPaid);

        if (allPaid) {
            this.status = SettlementStatus.COMPLETED;
        }
    }

    // 마지막 재촉 시간을 업데이트하는 도메인 메서드
    public void updateLastRemindedAt(LocalDateTime remindedAt) {
        this.lastRemindedAt = remindedAt;
    }
}
