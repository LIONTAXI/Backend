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


    // 추가 작성


    private Double latitude; // 현재 위도
    private Double longitude; // 현재 경도
    private LocalDateTime lastActiveAt; // 마지막으로 접속해서 활동한 시간
}