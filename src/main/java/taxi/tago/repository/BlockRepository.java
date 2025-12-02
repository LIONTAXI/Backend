package taxi.tago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxi.tago.entity.Block;
import taxi.tago.entity.User;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    // 내가 차단한 목록 조회
    List<Block> findAllByBlocker(User blocker);

    // 이미 차단했는지 확인, 중복 차단 방지
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    // 차단 해제할 때 사용, 차단한 사람 & 당한 사람으로 찾아서 삭제
    void deleteByBlockerAndBlocked(User blocker, User blocked);
}