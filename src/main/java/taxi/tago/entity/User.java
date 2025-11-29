package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 웹메일 (아이디로 사용)

    @Column(nullable = false, length = 255)
    private String password; // 비밀번호 (암호화된 값)

    // 추가 작성

    private Double latitude; // 현재 위도
    private Double longitude; // 현재 경도
    private LocalDateTime lastActiveAt; // 마지막으로 접속해서 활동한 시간
}