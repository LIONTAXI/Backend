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
import taxi.tago.service.TaxiPartyService;

// 채팅방 관련 HTTP API를 담당하는 컨트롤러
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "채팅방 API", description = "택시팟 단체 채팅방 생성 및 입장 기능을 제공합니다.")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final TaxiPartyService taxiPartyService;

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
            @RequestParam(name = "taxiPartyId") Long taxiPartyId, // 어떤 택시팟의 채팅방인지
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

    // 택시팟 끝내기 (채팅방 종료) API
    @PostMapping("/{chatRoomId}/close")
    @Operation(
            summary = "택시팟 끝내기 (채팅방 종료)",
            description = """
                    - 지난 택시팟 목록 화면에서 '택시팟 끝내기' 버튼을 눌렀을 때 호출되는 API입니다.
                    - 해당 채팅방(ChatRoom)의 closed 값을 true 로 바꿔,
                      이후 '내 채팅방 목록'(/api/chat/rooms/my) 조회 시 더 이상 노출되지 않도록 처리합니다.
                    - 택시팟의 총대슈니만 호출할 수 있으며,
                      총대가 아닌 사용자가 호출하면 400 Bad Request + 에러 메시지가 반환됩니다.
                    """
    )
    public ResponseEntity<String> closeChatRoom(
            @PathVariable(name = "chatRoomId") Long chatRoomId, // 어떤 채팅방을 끝낼지 식별하는 ID
            @AuthenticationPrincipal CustomUserDetails userDetails // JWT로부터 복원된 로그인 유저 정보
    ) {
        Long userId = userDetails.getUserId(); // 현재 로그인한 사용자의 PK

        // 비즈니스 로직 호출 (권한 체크 + ChatRoom.close() 수행)
        chatRoomService.closeChatRoom(chatRoomId, userId);

        // 별도의 DTO 필요없이 성공 메시지만 내려줌
        return ResponseEntity.ok("택시팟 채팅이 종료되었습니다.");
    }

    @PostMapping("/{taxiPartyId}/kick/{userId}")
    @Operation(
            summary = "동승슈니 강퇴하기 (총대 전용)",
            description = """
                총대슈니가 특정 동승슈니를 택시팟에서 강제로 내보냅니다.
                - 총대만 호출 가능
                - 강퇴된 멤버는 채팅도 불가
                - 상태는 ParticipationStatus.KICKED 로 기록됩니다.
                """
    )
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long taxiPartyId,
            @PathVariable("userId") Long targetUserId
    ) {
        Long hostId = userDetails.getUserId();
        taxiPartyService.kickMember(taxiPartyId, hostId, targetUserId);
        return ResponseEntity.ok().build();
    }
}
