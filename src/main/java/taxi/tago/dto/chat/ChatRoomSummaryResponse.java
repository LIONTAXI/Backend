package taxi.tago.dto.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import taxi.tago.constant.TaxiPartyStatus;
import taxi.tago.entity.ChatRoom;
import taxi.tago.entity.TaxiParty;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor // json 역직렬화
@AllArgsConstructor
public class ChatRoomSummaryResponse {

    private Long chatRoomId; // 채팅방 ID
    private Long taxiPartyId; // 택시팟 ID

    private String departure; // 출발지
    private String destination; // 도착지
    private String markerEmoji; // 이모지 (썸네일)

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime meetingTime; // 모임 시

    private String lastMessage; // 마지막 메시지
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt; // 마지막 메시지 시간

    private TaxiPartyStatus status; // MATCHING / FINISHED

    public static ChatRoomSummaryResponse from(ChatRoom room) {
        TaxiParty party = room.getTaxiParty();

        return new ChatRoomSummaryResponse(
                room.getId(),
                party.getId(),
                party.getDeparture(),
                party.getDestination(),
                party.getMarkerEmoji(),
                party.getMeetingTime(),
                room.getLastMessage(),
                room.getLastMessageAt(),
                party.getStatus()
        );
    }
}
