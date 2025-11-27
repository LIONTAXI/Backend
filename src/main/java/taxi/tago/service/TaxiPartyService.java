package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.dto.TaxiPartyDto;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.User;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxiPartyService {

    private final TaxiPartyRepository taxiPartyRepository;
    private final UserRepository userRepository;

    // 이모지 20개 리스트
    private static final List<String> EMOJI_LIST = Arrays.asList(
            "🐰", "🐹", "🍄", "⭐", "🐶", "🐱", "🦊", "🐻", "🐼", "🐨",
            "🐸", "♥️", "🦔", "🐢", "🐟", "🐬", "🐙", "🐥", "🦋", "🐌"
    );

    @Transactional
    public Long createTaxiParty(TaxiPartyDto.CreateRequest dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. id=" + dto.getUserId()));

        // 랜덤 이모지 선정
        String randomEmoji = getUniqueRandomEmoji();

        // 시간 결합 (오늘 날짜 + 입력받은 HH:mm)
        LocalDateTime meetingDateTime = LocalDateTime.of(LocalDate.now(), dto.getMeetingTime());

        // 엔티티 생성
        TaxiParty taxiParty = new TaxiParty(
                user,
                dto.getDeparture(),
                dto.getDestination(),
                meetingDateTime,
                dto.getMaxParticipants(),
                dto.getExpectedPrice(),
                dto.getContent(),
                randomEmoji
        );

        TaxiParty saved = taxiPartyRepository.save(taxiParty);
        return saved.getId();
    }

    // 택시팟 목록 조회 (매칭중 & 최신순)
    @Transactional(readOnly = true)
    public List<TaxiPartyDto.InfoResponse> getTaxiParties() {
        List<TaxiParty> parties = taxiPartyRepository.findAllByStatusOrderByCreatedAtDesc(TaxiPartyStatus.MATCHING);

        // 엔티티 -> DTO 변환
        return parties.stream()
                .map(party -> new TaxiPartyDto.InfoResponse(
                        party.getId(),
                        party.getDeparture(),
                        party.getDestination(),
                        party.getMeetingTime().toLocalTime(),
                        party.getCurrentParticipants(),
                        party.getMaxParticipants(),
                        party.getExpectedPrice()
                ))
                .collect(Collectors.toList());
    }

    // 이모지 중복 방지 및 랜덤 추출 로직
    private String getUniqueRandomEmoji() {
        // 현재 '매칭 중'인 글에서 사용 중인 이모지들을 가져옴
        List<String> usedEmojis = taxiPartyRepository.findAllEmojisByStatus(TaxiPartyStatus.MATCHING);

        // 전체 리스트에서 사용 중인 이모지 제외
        List<String> availableEmojis = EMOJI_LIST.stream()
                .filter(emoji -> !usedEmojis.contains(emoji))
                .collect(Collectors.toList());

        Random random = new Random();

        // 20개 다 사용 중일 경우
        if (availableEmojis.isEmpty()) {
            // 다시 전체 이모지 리스트 중에서 랜덤 선정
            return EMOJI_LIST.get(random.nextInt(EMOJI_LIST.size()));
        }

        // 전체 이모지 리스트 중에서 랜덤 선정
        return availableEmojis.get(random.nextInt(availableEmojis.size()));
    }
}