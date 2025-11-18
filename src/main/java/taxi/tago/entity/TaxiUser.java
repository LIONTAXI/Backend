package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "taxi_member")
public class TaxiUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "taxiuser_id")
    private Long id;

    // 어떤 택시팟인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxiparty_id", nullable = false)
    private TaxiParty taxiParty;

    // 택시팟 참여자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // 생성자
    public TaxiUser(TaxiParty taxiParty, User user) {
        this.taxiParty = taxiParty;
        this.user = user;
    }
}
