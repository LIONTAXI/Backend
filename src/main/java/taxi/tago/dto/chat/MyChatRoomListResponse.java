package taxi.tago.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyChatRoomListResponse {
    // 지금 매칭중인 택시팟 (위쪽 섹션)
    private List<ChatRoomSummaryResponse> matchingRooms;

    // 지난 택시팟 (이래쪽 섹션)
    private List<ChatRoomSummaryResponse> finishedRooms;
}
