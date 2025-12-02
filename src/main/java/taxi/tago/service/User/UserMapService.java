package taxi.tago.service.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.dto.UserMapDto;
import taxi.tago.entity.Block;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.User;
import taxi.tago.repository.BlockRepository;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMapService {

    private final UserRepository userRepository;
    private final TaxiPartyRepository taxiPartyRepository;
    private final BlockRepository blockRepository;

    // 유저 위치 및 마지막 활동 시간 업데이트
    @Transactional
    public void userMapUpdate(UserMapDto.UpdateRequest dto) {
        // userId로 유저 찾기
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. id=" + dto.getUserId()));

        // 위치 정보 업데이트
        user.setLatitude(dto.getLatitude());
        user.setLongitude(dto.getLongitude());

        // 현재 시간을 마지막 활동 시간으로 기록
        user.setLastActiveAt(LocalDateTime.now());
    }

    // 현재 접속 중인 유저 조회 (마지막 활동 시간이 3분 이내)
    @Transactional(readOnly = true)
    public List<UserMapDto.Response> getActiveUsers(Long myId) { // 파라미터로 myId 받기
        User me = userRepository.findById(myId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        // 차단 리스트: 내가 차단한 사람 + 나를 차단한 사람
        List<Block> blocksFromMe = blockRepository.findAllByBlocker(me);
        List<Block> blocksToMe = blockRepository.findAllByBlocked(me);

        // 안 보여줄 사람들의 ID 집합
        Set<Long> invisibleUserIds = blocksFromMe.stream()
                .map(block -> block.getBlocked().getId())
                .collect(Collectors.toSet());

        invisibleUserIds.addAll(blocksToMe.stream()
                .map(block -> block.getBlocker().getId())
                .collect(Collectors.toList()));

        // 접속 중인 유저 조회
        List<User> users = userRepository.findAll();
        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);

        // 현재 '매칭 중'인 택시팟 정보
        List<TaxiParty> activeParties = taxiPartyRepository.findAllByStatusOrderByCreatedAtDesc(TaxiPartyStatus.MATCHING);
        Map<Long, String> hostEmojiMap = activeParties.stream()
                .filter(party -> party.getUser() != null)
                .collect(Collectors.toMap(
                        party -> party.getUser().getId(),
                        TaxiParty::getMarkerEmoji,
                        (oldEmoji, newEmoji) -> oldEmoji
                ));

        return users.stream()
                .filter(user -> user.getLatitude() != null
                        && user.getLongitude() != null
                        && user.getLastActiveAt() != null
                        && user.getLastActiveAt().isAfter(threeMinutesAgo))
                .filter(user -> !invisibleUserIds.contains(user.getId())) // 차단 목록에 없는 사람만 표시
                .map(user -> {
                    String emoji = hostEmojiMap.getOrDefault(user.getId(), "👤");
                    return new UserMapDto.Response(
                            user.getId(),
                            user.getLatitude(),
                            user.getLongitude(),
                            emoji
                    );
                })
                .collect(Collectors.toList());
    }
}