package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import taxi.tago.constant.TaxiPartyStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaxiParty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "taxiparty_id")
    private Long id;

    // 총대슈니 테이블 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 승차지
    @Column(name = "departure", nullable = false, length = 50)
    private String departure;

    // 하차지
    @Column(name = "destination", nullable = false, length = 50)
    private String destination;

    // 모집 마감 시각
    @Column(name = "meeting_time", nullable = false)
    private LocalDateTime meetingTime;

    // 모집 인원
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    // 현재 모인 인원 (기본값 1)
    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants = 1;

    // 예상 가격
    @Column(name = "expected_price", nullable = false)
    private Integer expectedPrice;

    // 추가 설명
    @Column(name = "content", length = 100)
    private String content;

    // 총대슈니 마커 이모지
    @Column(name = "marker_emoji", nullable = false, length = 20)
    private String markerEmoji;

    // 매칭 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaxiPartyStatus status;

    // 생성 일시
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    // 생성자
    public TaxiParty(User user, String departure, String destination, LocalDateTime meetingTime,
                     Integer maxParticipants, Integer expectedPrice, String content, String markerEmoji) {
        this.user = user;
        this.departure = departure;
        this.destination = destination;
        this.meetingTime = meetingTime;
        this.maxParticipants = maxParticipants;
        this.expectedPrice = expectedPrice;
        this.content = content;
        this.markerEmoji = markerEmoji;
        this.status = TaxiPartyStatus.MATCHING; // 기본값: 매칭중
        this.currentParticipants = 1;
    }
}