package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import taxi.tago.dto.chat.ChatRoomResponse;
import taxi.tago.dto.chat.MyChatRoomListResponse;
import taxi.tago.entity.ChatRoom;
import taxi.tago.security.CustomUserDetails;
import taxi.tago.service.ChatRoomService;

// 채팅방 관련 HTTP API를 담당하는 컨트롤러
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "채팅방 API", description = "택시팟 단체 채팅방 생성 및 입장 기능을 제공합니다.")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 입장 또는 생성 API
    @PostMapping("/enter")
    @Operation(
            summary = "채팅방 입장 또는 생성",
            description = """
                    - 택시팟 ID와 JWT 로그인 정보를 기준으로 채팅방에 입장합니다.
                    - 채팅방이 이미 존재하면 그대로 입장하고,
                    - 아직 채팅방이 없으면 새 채팅방을 생성한 뒤 그 방으로 입장합니다.
                    - 입장 권한이 없는 유저(총대 X, ACCEPTED 동승슈니 X)는 400 에러가 발생합니다.
                    """
    )
    public ResponseEntity<ChatRoomResponse> enterChatRoom(
            @RequestParam Long taxiPartyId, // 어떤 택시팟의 채팅방인지
            @AuthenticationPrincipal CustomUserDetails userDetails  // 로그인한 사용자 정보
    ) {
        Long userId = userDetails.getUserId(); // JWT에서 복원한 로그인 유저의 ID

        ChatRoom room = chatRoomService.enterOrCreateChatRoom(taxiPartyId, userId);

        // 엔티티 -> DTO 변환
        ChatRoomResponse response = ChatRoomResponse.from(room);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(
            summary = "내 채팅방 목록 조회",
            description = """
                    - 로그인한 사용자가 참여한 택시팟의 채팅방 목록을 조회합니다.
                    - TaxiPartyStatus와 ChatRoom.closed 값을 조합해서
                      '지금 매칭중인 택시팟' / '지난 택시팟' 두 영역으로 나눠서 응답합니다.
                    - '택시팟 끝내기' 버튼을 누른 채팅방(room.closed = true)은
                      목록에서 완전히 제외됩니다.
                    """
    )
    public ResponseEntity<MyChatRoomListResponse> getMyChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        MyChatRoomListResponse response = chatRoomService.getMyChatRooms(userId);
        return ResponseEntity.ok(response);
    }
}
