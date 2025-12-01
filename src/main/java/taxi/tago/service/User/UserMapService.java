package taxi.tago.service.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.dto.UserMapDto;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.User;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMapService {

    private final UserRepository userRepository;
    private final TaxiPartyRepository taxiPartyRepository;

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
    public List<UserMapDto.Response> getActiveUsers() {
        List<User> users = userRepository.findAll();

        // 현재 '매칭 중'인 택시팟
        List<TaxiParty> activeParties = taxiPartyRepository.findAllByStatusOrderByCreatedAtDesc(TaxiPartyStatus.MATCHING);

        Map<Long, String> hostEmojiMap = activeParties.stream()
                .filter(party -> party.getUser() != null) // 유저 없는 방 오류 예방
                .collect(Collectors.toMap(
                        party -> party.getUser().getId(),
                        TaxiParty::getMarkerEmoji,
                        (oldEmoji, newEmoji) -> oldEmoji
                ));


        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3); // 마지막 접속 시간 조절 여기에서 !

        return users.stream()
                // 활동 중인 유저 필터링
                .filter(user -> user.getLatitude() != null
                        && user.getLongitude() != null
                        && user.getLastActiveAt() != null
                        && user.getLastActiveAt().isAfter(threeMinutesAgo))
                .map(user -> {
                    // 이 유저가 총대 명단(Map)에 있으면 그 이모지, 없으면 기본값(👤) 사용
                    String emoji = hostEmojiMap.getOrDefault(user.getId(), "👤");

                    return UserMapDto.Response.of(
                            user.getId(),
                            user.getLatitude(),
                            user.getLongitude(),
                            emoji
                    );
                })
                .collect(Collectors.toList());
    }
}