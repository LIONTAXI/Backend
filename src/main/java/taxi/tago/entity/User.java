package taxi.tago.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import taxi.tago.constant.UserRole;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER; // 사용자 역할 (ADMIN, USER)

    // 추가 작성

    private Double latitude; // 현재 위도
    private Double longitude; // 현재 경도
    private LocalDateTime lastActiveAt; // 마지막으로 접속해서 활동한 시간
    
    @Column(length = 20)
    private String studentId; // 학번 (도서관 전자출입증 인증용)
    
    @Column(length = 50)
    private String name; // 이름 (도서관 전자출입증 인증용)
}