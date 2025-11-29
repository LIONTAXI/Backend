package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.LibraryCardAuth;
import taxi.tago.entity.User;

import java.util.List;
import java.util.Optional;

public interface LibraryCardAuthRepository extends JpaRepository<LibraryCardAuth, Long> {
    List<LibraryCardAuth> findByUserOrderByCreatedAtDesc(User user);
    Optional<LibraryCardAuth> findFirstByUserAndIsSuccessTrueOrderByCreatedAtDesc(User user);
}

