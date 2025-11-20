package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}