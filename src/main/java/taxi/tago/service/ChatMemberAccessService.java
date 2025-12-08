package taxi.tago.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import taxi.tago.constant.ParticipationStatus;
import taxi.tago.entity.TaxiParty;
import taxi.tago.entity.TaxiUser;
import taxi.tago.repository.TaxiUserRepository;

import java.util.Optional;

// 채팅방/채팅메시지에서 공통으로 사용하는 validateChatMember() (채팅 참여 자격 검증 로직) 메서드를 모아둔 클래스
@Service
@RequiredArgsConstructor
public class ChatMemberAccessService {

    private final TaxiUserRepository taxiUserRepository;

    // 주어진 택시팟 + 유저 조합이 채팅에 참여할 자격이 있는지 여부를 반환하는 메서드
    public boolean hasChatPermission(TaxiParty taxiParty, Long userId) {
        // 택시팟의 총대슈니인지 먼저 확인
        boolean isHost = taxiParty.getUser().getId().equals(userId);
        if (isHost) {
            return true;
        }

        // 동승슈니로 참여했는지 조회
        Optional<TaxiUser> taxiUserOpt = taxiUserRepository.findByTaxiPartyIdAndUserId(taxiParty.getId(), userId);

        // 그 중에서도 상태가 ACCEPTED인지 확인
        boolean isAcceptedPassenger = taxiUserOpt
                .filter(tu -> tu.getStatus() == ParticipationStatus.ACCEPTED)
                .isPresent();

        return isAcceptedPassenger;
    }
}
