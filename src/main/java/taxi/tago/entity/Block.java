package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성 시간 자동 기록용
@Table(
        name = "blocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_blocker_blocked",
                        columnNames = {"blocker_id", "blocked_id"} // 중복 차단 방지
                )
        }
)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long id;

    // 차단을 '한' 사람 (나)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    // 차단을 '당한' 사람 (상대방)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 생성자
    public Block(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}