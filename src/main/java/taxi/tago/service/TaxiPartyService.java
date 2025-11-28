package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.dto.TaxiPartyDto;
import taxi.tago.dto.TaxiUserDto;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.TaxiUser;
import taxi.tago.entity.User;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.TaxiUserRepository;
import taxi.tago.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxiPartyService {

    private final TaxiPartyRepository taxiPartyRepository;
    private final UserRepository userRepository;
    private final TaxiUserRepository taxiUserRepository;

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

    // 택시팟 정보
    @Transactional(readOnly = true)
    public TaxiPartyDto.DetailResponse getTaxiPartyDetail(Long taxiPartyId, Long userId) {
        // ID로 택시팟 찾기
        TaxiParty party = taxiPartyRepository.findById(taxiPartyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다. id=" + taxiPartyId));

        // 나의 참여 상태 기본값: 참여 안 함
        String myStatus = "NONE"; //

        // 내가 신청한 기록이 있는지 조회
        Optional<TaxiUser> myRequest = taxiUserRepository.findByTaxiPartyIdAndUserId(taxiPartyId, userId);

        if (myRequest.isPresent()) {
            // 기록이 있으면 WAITING 또는 ACCEPTED 가져옴
            myStatus = myRequest.get().getStatus().toString();
        }

        // 엔티티 -> 상세 DTO 변환
        return new TaxiPartyDto.DetailResponse(
                party.getId(),
                party.getUser().getId(),
                party.getDeparture(),
                party.getDestination(),
                party.getMeetingTime().toLocalTime(),
                party.getCurrentParticipants(),
                party.getMaxParticipants(),
                party.getExpectedPrice(),
                party.getContent(),
                party.getStatus().toString(),
                myStatus
        );
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

    // 택시팟 정보 - 동승슈니 - 같이 타기
    @Transactional
    public String applyTaxiParty(Long partyId, Long userId) {
        // 확인, 예외 처리 로직
        TaxiParty party = taxiPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다."));
        if (party.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 택시팟의 총대슈니입니다.");
        }
        if (taxiUserRepository.existsByTaxiPartyIdAndUserId(partyId, userId)) {
            throw new IllegalArgumentException("이미 요청을 보낸 택시팟입니다.");
        }

        // 요청 정보 저장
        TaxiUser taxiUser = new TaxiUser(party, user);
        taxiUserRepository.save(taxiUser);

        return "같이 타기 요청이 완료되었습니다.";
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 조회=
    @Transactional(readOnly = true)
    public List<TaxiUserDto.RequestResponse> getJoinRequests(Long partyId) {
        // 요청 보낸 동승슈니 조회
        List<TaxiUser> requests = taxiUserRepository.findAllByTaxiPartyId(partyId);

        // DTO 변환
        return requests.stream()
                .map(request -> new TaxiUserDto.RequestResponse(
                        request.getId(),
                        request.getUser().getId(),
                        request.getStatus()
                ))
                .collect(Collectors.toList());
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 수락
    @Transactional
    public String acceptJoinRequest(Long taxiUserId) {
        TaxiUser taxiUser = taxiUserRepository.findById(taxiUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        // 해당 동승슈니의 같이 타기 요청 수락
        taxiUser.setStatus(ParticipationStatus.ACCEPTED);

        // 택시팟의 현재 인원 +1
        TaxiParty party = taxiUser.getTaxiParty();
        party.setCurrentParticipants(party.getCurrentParticipants() + 1);

        Long acceptedUserId = taxiUser.getUser().getId();
        return "같이 타기 요청 수락 성공, 수락한 동승슈니 ID: " + acceptedUserId;
    }
}