package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// 택시팟 후기를 저장하는 엔티티
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_taxiParty_reviewer_reviewee",
                        columnNames = {"taxiparty_id", "reviewer_id", "reviewee_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    // 어떤 택시팟에서 발생한 후기인지
    // Review (N) : TaxiParty (1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxiparty_id", nullable = false)
    private TaxiParty taxiParty;

    // 후기 작성자
    // Review (N) : User[reviewer] (1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // 후기 대상자
    // Review (N) : User[reviewee] (1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    // 리뷰 작성자가 총대슈니인지 아닌지 여부
    // true == 총대슈니
    @Column(name = "reviewer_is_host", nullable = false)
    private boolean reviewerIsHost;

    // 긍정 후기 태그 목록
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_positive_tags",
            joinColumns = @JoinColumn(name = "review_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", length = 50, nullable = false)
    private Set<PositiveReviewTag> positiveTags = new HashSet<>();

    // 부정 후기 태그 목록
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_negative_tags",
            joinColumns = @JoinColumn(name = "review_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", length = 50, nullable = false)
    private Set<NegativeReviewTag> negativeTags = new HashSet<>();

    // 재매칭 희망 여부
    @Column(name = "want_to_meet_again", nullable = false)
    private boolean wantToMeetAgain;

    // 리뷰 작성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 리뷰 생성 전용 정적 팩토리 메서드 (생성자 대신 사용해서 필수 값의 누락/검증을 통제)
    public static Review create(
            TaxiParty taxiParty,
            User reviewer,
            User reviewee,
            boolean reviewerIsHost,
            boolean wantToMeetAgain,
            Set<PositiveReviewTag> positiveTags,
            Set<NegativeReviewTag> negativeTags
    ) {
        if (taxiParty == null) {
            throw new IllegalArgumentException("리뷰 생성 시 TaxiParty는 필수입니다.");
        }
        if (reviewer == null) {
            throw new IllegalArgumentException("리뷰 작성자는 필수입니다.");
        }
        if (reviewee == null) {
            throw new IllegalArgumentException("리뷰 대상자는 필수입니다.");
        }
        if (reviewer.getId().equals(reviewee.getId())) {
            throw new IllegalArgumentException("본인에게 후기를 남길 수 없습니다.");
        }
        if (positiveTags == null || positiveTags.isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상의 긍정 태그를 선택해야 합니다.");
        }

        Review review = new Review();
        review.taxiParty = taxiParty;
        review.reviewer = reviewer;
        review.reviewee = reviewee;
        review.reviewerIsHost = reviewerIsHost;
        review.wantToMeetAgain = wantToMeetAgain;

        // 방어적 복사로 외부 컬렉션과 내부 컬렉션 분리
        review.positiveTags.addAll(positiveTags);
        if (negativeTags != null) {
            review.negativeTags.addAll(negativeTags);
        }
        return review;
    }


    // 긍정 후기 태그 enum
    public enum PositiveReviewTag {
        // 공통
        PROMISE_ON_TIME, // 약속을 잘 지켜요
        RESPONSE_FAST, // 응답이 빨라요
        GOOD_MANNER, // 매너가 좋아요

        // 동승슈니 대상
        SETTLEMENT_FAST, // 정산이 빨라요
        KIND, // 친절해요

        // 총대슈니 대상
        INFO_NOTICE_FAST, // 정보 공지가 빨라요
        INFO_ACCURATE // 정산 정보가 정확해요
    }

    // 부정 후기 태그 enum
    public enum NegativeReviewTag {
        // 공통
        PROMISE_NOT_KEPT, // 약속시간을 지키지 않았어요
        COMMUNICATION_HARD, // 소통이 어려웠어요
        MANNER_BAD, // 매너가 좋지 않았어요

        // 동승슈니 대상
        SETTLEMENT_LATE, // 정산이 느렸어요

        // 총대슈니 전용
        INFO_INACCURATE // 정산 정보가 정확하지 않았어요
    }
}
