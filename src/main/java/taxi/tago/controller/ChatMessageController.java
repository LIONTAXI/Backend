package taxi.tago.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import taxi.tago.dto.chat.ChatMessageResponse;
import taxi.tago.dto.chat.ChatMessageSendRequest;
import taxi.tago.security.CustomUserDetails;
import taxi.tago.service.ChatMessageService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.List;
import java.util.Map;

// 채팅 메시지 관련 HTTP + WebSocket 엔드포인트를 담당하는 컨트롤러
@Slf4j
@RestController // HTTP 응답은 JSON으로 내려주고, STOMP 메시지는 WebSocket용으로 처리
@RequiredArgsConstructor
@Tag(name = "채팅 메시지 API", description = "택시팟 채팅방 메시지 전송 및 조회 기능을 제공합니다.")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate; // 서버 -> 클라이언트 브로드캐스트 용도

    // WebSocket/STOMP 기반 채팅 메시지 전송 엔드포인트
    // HTTP 관점이 아니라, STOMP 프레임 기준으로 -> 클라이언트가 /app/chat/send로 SEND 하면 이 메서드가 호출됨
    @MessageMapping("/chat/send")
    public void sendChatMessage(
            ChatMessageSendRequest request, // 클라이언트 -> 서버로 넘어온 메시지 내용
            Principal principal, // Handshake에서 심어둔 Principal (email 등)
            StompHeaderAccessor headerAccessor // WebSocket 세션 속성 접근용
    ) {
        // HandshakeInterceptor에서 WebSocket 세션에 저장해둔 CustomUserDetails 꺼내기
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        CustomUserDetails userDetails = null;

        if (sessionAttributes != null) {
            Object attr = sessionAttributes.get("userDetails");
            if (attr instanceof CustomUserDetails details) {
                userDetails = details;
            }
        }

        if (userDetails == null) {
            // JWT 인증없이 HandshakeInterceptor를 통과해서 들어온 비정상 요청 방어
            log.warn("WebSocket 메시지 전송 시 인증 정보가 없습니다. principal = {}", principal);
            return;
        }

        Long senderId = userDetails.getUserId();

        // 서비스 계층에 위임해서 권한 검증, 메시지 엔티티 생성 및 저장, ChatMessageResponse DTO 생성
        ChatMessageResponse response = chatMessageService.sendTextMessage(request, senderId);

        // 해당 채팅방을 구독 중인 모든 클라이언트에게 브로드캐스트
        // (클라이언트는 "/topic/chatrooms/{chatRoomId}"를 구독하고 있어야 함)
        String destination = "/topic/chatrooms/" + response.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        // 로그 남기기
        log.info("채팅 메시지 전송: roomId={}, senderId={}, content={}",
                response.getChatRoomId(), response.getSenderId(), response.getContent());
    }

    // HTTP API: 특정 채팅방의 메시지 목록 조회
    @GetMapping("/api/chat/rooms/{chatRoomId}/messages")
    @Operation(
            summary = "채팅방 메시지 목록 조회",
            description = """
                    - 특정 채팅방에 쌓인 메시지들을 시간순으로 조회합니다.
                    - 클라이언트는 채팅방 최초 입장 시, 이 API를 한 번 호출해서
                      기존 대화 내용을 불러온 뒤,
                    - 이후부터는 WebSocket(STOMP) 실시간 메시지만 화면에 추가하면 됩니다.
                    - 채팅방 멤버가 아닌 유저가 조회를 시도할 경우 400 에러를 반환합니다.
                    """
    )
    public List<ChatMessageResponse> getChatMessages(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails userDetails // JWT 기반 인증 정보
    ) {
        Long userId = userDetails.getUserId(); // JWT에서 복원된 현재 로그인 유저 ID
        return chatMessageService.getMessages(chatRoomId, userId);
    }
}
