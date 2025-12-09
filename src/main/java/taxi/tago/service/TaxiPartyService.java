package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.dto.TaxiPartyDto;
import taxi.tago.dto.TaxiUserDto;
import taxi.tago.entity.Block;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.TaxiUser;
import taxi.tago.entity.User;
import taxi.tago.repository.BlockRepository;
import taxi.tago.repository.ChatRoomRepository;
import taxi.tago.repository.TaxiPartyRepository;
import taxi.tago.repository.TaxiUserRepository;
import taxi.tago.repository.UserRepository;
import taxi.tago.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxiPartyService {

    private final TaxiPartyRepository taxiPartyRepository;
    private final UserRepository userRepository;
    private final TaxiUserRepository taxiUserRepository;
    private final BlockRepository blockRepository;
    private final NotificationService notificationService;
    private final ChatRoomRepository chatRoomRepository;

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

    // 택시팟 목록 조회 (매칭중 & 최신순 & 차단 필터링)
        @Transactional(readOnly = true)
        public List<TaxiPartyDto.InfoResponse> getTaxiParties(Long myId) { // 파라미터로 myId 받기
            User me = userRepository.findById(myId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

            // 차단 리스트
            List<Block> blocksFromMe = blockRepository.findAllByBlocker(me);
            List<Block> blocksToMe = blockRepository.findAllByBlocked(me);

            Set<Long> invisibleUserIds = blocksFromMe.stream()
                    .map(block -> block.getBlocked().getId())
                    .collect(Collectors.toSet());

            invisibleUserIds.addAll(blocksToMe.stream()
                    .map(block -> block.getBlocker().getId())
                    .collect(Collectors.toList()));

            // 전체 매칭중 리스트 가져오기
            List<TaxiParty> parties = taxiPartyRepository.findAllByStatusOrderByCreatedAtDesc(TaxiPartyStatus.MATCHING);

            // 필터링 및 변환
            return parties.stream()
                    .filter(party -> !invisibleUserIds.contains(party.getUser().getId())) // 작성자가 차단 목록에 있으면 제외
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

        // 알림 전송
        Long hostId = party.getUser().getId();
        String requesterName = user.getName() != null ? user.getName() : "동승슈니";

        try {
            notificationService.sendTaxiParticipationRequest(hostId, partyId, requesterName);
        } catch (Exception e) {
            System.err.println("알림 전송 중 오류 발생 (같이타기 요청은 성공): " + e.getMessage());
        }

        return "같이 타기 요청이 완료되었습니다.";
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 참여 요청 조회
    @Transactional(readOnly = true)
    public List<TaxiUserDto.RequestResponse> getJoinRequests(Long partyId) {
        // 요청 보낸 동승슈니 조회
        List<TaxiUser> requests = taxiUserRepository.findAllByTaxiPartyId(partyId);

        // DTO 변환
        return requests.stream()
                .map(request -> new TaxiUserDto.RequestResponse(
                        request.getId(),
                        request.getUser().getId(),
                        request.getUser().getName(),
                        request.getUser().getShortStudentId(),
                        request.getUser().getImgUrl(),
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

        // 수락된 동승슈니에게 알림 보내기
        Long acceptedUserId = taxiUser.getUser().getId();
        Long taxiPartyId = party.getId();
        
        // 채팅방 ID 조회 (채팅방이 없을 수도 있으므로 Optional 처리)
        Long roomId = chatRoomRepository.findByTaxiPartyId(taxiPartyId)
                .map(room -> room.getId())
                .orElse(null); // 채팅방이 없으면 null
        
        String hostName = party.getUser().getName() != null ? party.getUser().getName() : "총대슈니";
        
        // 채팅방이 존재하는 경우에만 알림 전송 (채팅방이 없으면 roomId가 null이므로 알림은 보내지 않음)
        if (roomId != null) {
            notificationService.sendTaxiParticipationAccepted(acceptedUserId, roomId, hostName);
        }

        return "같이 타기 요청 수락 성공, 수락한 동승슈니 ID: " + acceptedUserId;
    }

    // 택시팟 상세페이지 - 총대슈니 - 매칭 종료
    @Transactional
    public String closeTaxiParty(Long partyId, Long userId) {
        // 매칭 종료할 택시팟 찾기
        TaxiParty party = taxiPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다."));

        // 권한 확인 (총대슈니만 종료 가능)
        if (!party.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("총대슈니만 매칭을 종료할 수 있습니다.");
        }

        // 상태 변경
        party.setStatus(TaxiPartyStatus.FINISHED);

        return "매칭이 종료되었습니다. 택시팟 ID: " + partyId;
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 삭제
    @Transactional
    public String deleteTaxiParty(Long partyId, Long userId) {
        TaxiParty party = taxiPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다."));

        // 해당 택시팟의 총대슈니만 삭제 가능
        if (!party.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("총대슈니만 삭제할 수 있습니다.");
        }

        // 총대슈니 이외의 동승슈니가 1명이라도 있으면 삭제 불가
        if (party.getCurrentParticipants() >= 2) {
            throw new IllegalArgumentException("동승슈니가 있어 삭제할 수 없습니다.");
        }

        // 해당 택시팟의 모든 요청 내역 삭제 (외래키 제약조건 에러 방지)
        List<TaxiUser> requests = taxiUserRepository.findAllByTaxiPartyId(partyId);
        taxiUserRepository.deleteAll(requests);

        // 택시팟 삭제
        taxiPartyRepository.delete(party);

        return "택시팟 삭제가 완료되었습니다. ID: " + partyId;
    }

    // 택시팟 상세페이지 - 총대슈니 - 택시팟 수정
    @Transactional
    public String updateTaxiParty(Long partyId, TaxiPartyDto.UpdateRequest dto) {
        TaxiParty party = taxiPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 택시팟이 존재하지 않습니다. id=" + partyId));

        if (!party.getUser().getId().equals(dto.getUserId())) {
            throw new IllegalArgumentException("총대슈니만 수정할 수 있습니다.");
        }


        LocalDateTime meetingDateTime = LocalDateTime.of(LocalDate.now(), dto.getMeetingTime());

        // 데이터 업데이트
        party.setDeparture(dto.getDeparture());
        party.setDestination(dto.getDestination());
        party.setMeetingTime(meetingDateTime);
        party.setMaxParticipants(dto.getMaxParticipants());
        party.setExpectedPrice(dto.getExpectedPrice());
        party.setContent(dto.getContent());

        return "수정이 완료되었습니다. ID: " + partyId;
    }
}