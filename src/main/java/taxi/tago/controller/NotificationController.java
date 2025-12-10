package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import taxi.tago.dto.NotificationDto;
import taxi.tago.service.NotificationService;
import taxi.tago.util.SseEmitters;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 API", description = "알림 목록 조회, 실시간 알림 스트림, 알림 읽음 처리 기능을 제공합니다.")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitters sseEmitters;

    
    @GetMapping
    @Operation(
            summary = "알림 목록 조회",
            description = "최신 알림부터 내림차순으로 반환합니다. 페이지네이션을 지원합니다. (기본값: size=20, sort=createdAt, direction=DESC)"
    )
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @RequestParam(name = "userId") Long userId, // 현재 사용자 ID (쿼리 파라미터)
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable // 페이지네이션 정보 (기본값: size=20, sort=createdAt, direction=DESC)
    ) {
        Page<NotificationDto> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(notifications); // 알림 목록 반환
    }

    // 미확인 알림 개수 조회 API
    @GetMapping("/unread-count")
    @Operation(
            summary = "미확인 알림 개수 조회",
            description = "벨 배지에 표시할 미확인 알림 개수를 조회합니다."
    )
    public ResponseEntity<Long> getUnreadCount(@RequestParam(name = "userId") Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    
     // SSE 실시간 알림 스트림 연결 API
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "SSE 실시간 알림 스트림 연결",
            description = "클라이언트가 이 엔드포인트에 연결하면 서버에서 알림이 발생할 때마다 실시간으로 이벤트를 전송합니다."
    )
    public SseEmitter streamNotifications(@RequestParam(name = "userId") Long userId) { // 현재 사용자 ID (쿼리 파라미터)
        SseEmitter emitter = sseEmitters.create(userId); // SSE 연결 생성
        
        // 연결 즉시 초기 이벤트 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결이 성공적으로 설정되었습니다."));
            log.info("SSE 초기 연결 이벤트 전송 성공: userId={}", userId);
        } catch (Exception e) {
            log.error("SSE 초기 연결 이벤트 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
            emitter.completeWithError(e);
        }
        
        return emitter; // SSE 연결 반환
    }

    // 알림 읽음 처리 API
    @PatchMapping("/{id}/read")
    @Operation(
            summary = "알림 읽음 처리",
            description = "사용자가 알림 카드를 클릭하면 호출됩니다. read = true로 변경되어 UI에서 '읽은 알림' 스타일로 표시됩니다."
    )
    public ResponseEntity<Void> markAsRead(
            @PathVariable(name = "id") Long id, // 알림 ID (Path Variable)
            @RequestParam(name = "userId") Long userId // 현재 사용자 ID (쿼리 파라미터)
    ) {
        notificationService.markAsRead(id, userId); // 알림 읽음 처리
        return ResponseEntity.ok().build(); // 성공 응답
    }
}

