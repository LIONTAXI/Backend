package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "library_card_auth")
@Getter
@Setter
@NoArgsConstructor
public class LibraryCardAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String extractedName; // OCR로 추출한 이름

    @Column(length = 20)
    private String extractedStudentId; // OCR로 추출한 학번

    @Column(nullable = false)
    private Boolean isSuccess; // 인증 성공 여부

    @Column(length = 500)
    private String failureReason; // 실패 사유

    @Column(nullable = false)
    private LocalDateTime createdAt; // 인증 시도 시간

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

