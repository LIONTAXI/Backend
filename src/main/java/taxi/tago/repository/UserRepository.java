package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.constant.UserRole;
import taxi.tago.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // 이메일과 역할로 사용자 조회 (회원가입 시 USER role만 체크하기 위해)
    Optional<User> findByEmailAndRole(String email, UserRole role);
}