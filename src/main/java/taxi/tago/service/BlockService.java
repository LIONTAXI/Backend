package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.dto.BlockDto;
import taxi.tago.entity.Block;
import taxi.tago.entity.User;
import taxi.tago.repository.BlockRepository;
import taxi.tago.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    // 차단하기
    @Transactional
    public String blockUser(BlockDto.BlockRequest dto) {
        // 자기 자신 차단 불가
        if (dto.getBlockerId().equals(dto.getBlockedId())) {
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }

        // 사용자 조회
        User blocker = userRepository.findById(dto.getBlockerId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. (나)"));
        User blocked = userRepository.findById(dto.getBlockedId())
                .orElseThrow(() -> new IllegalArgumentException("차단할 사용자를 찾을 수 없습니다. (상대방)"));

        // 이미 차단한 상태인지 확인 (중복 방지)
        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new IllegalArgumentException("이미 차단한 사용자입니다.");
        }

        // 차단 저장
        Block block = new Block(blocker, blocked);
        blockRepository.save(block);

        return "차단이 완료되었습니다. (본인 ID: " + blocker.getId() + ", 차단한 상대방 ID: " + blocked.getId() + ")";
    }
}