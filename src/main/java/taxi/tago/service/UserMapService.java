package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.dto.UserMapDto;
import taxi.tago.entity.User;
import taxi.tago.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMapService {

    private final UserRepository userRepository;

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
        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3); // 접속 시간 조절 여기에서!

        return users.stream()
                .filter(user -> user.getLatitude() != null
                        && user.getLongitude() != null
                        && user.getLastActiveAt() != null
                        && user.getLastActiveAt().isAfter(threeMinutesAgo))
                .map(user -> UserMapDto.Response.from(
                        user.getId(),
                        user.getLatitude(),
                        user.getLongitude()
                ))
                .collect(Collectors.toList());
    }
}