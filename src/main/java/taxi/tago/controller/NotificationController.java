package taxi.tago.controller;

import lombok.RequiredArgsConstructor;
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

/**
 * 알림 컨트롤러
 * 
 * 알림 관련 REST API를 제공합니다.
 * 
 * API 엔드포인트:
 * - GET /api/notifications: 알림 목록 조회 (페이지네이션)
 * - GET /api/notifications/unread-count: 미확인 알림 개수 조회 (벨 배지용)
 * - GET /api/notifications/stream: SSE 실시간 알림 스트림 연결
 * - PATCH /api/notifications/{id}/read: 알림 읽음 처리
 * 
 * 주의사항:
 * - 현재는 userId를 쿼리 파라미터로 받지만, 실제 프로젝트에서는
 *   JWT 토큰이나 세션에서 현재 사용자 ID를 추출하는 방식으로 변경해야 합니다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitters sseEmitters;

    /**
     * 알림 목록 조회
     * 
     * 최신 알림부터 내림차순으로 반환합니다.
     * 프론트에서 알림 센터 화면에 표시할 데이터입니다.
     * 
     * @param userId 현재 사용자 ID (쿼리 파라미터)
     * @param pageable 페이지네이션 정보 (기본값: size=20, sort=createdAt, direction=DESC)
     * @return 알림 목록 (Page)
     * 
     * 예시 요청:
     * GET /api/notifications?userId=1&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @RequestParam Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<NotificationDto> notifications = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 미확인 알림 개수 조회
     * 
     * 벨 아이콘 배지에 표시할 숫자입니다.
     * 프론트에서 주기적으로(예: 10초마다) 폴링하여 최신 상태를 유지합니다.
     * 
     * @param userId 현재 사용자 ID (쿼리 파라미터)
     * @return 미확인 알림 개수
     * 
     * 예시 요청:
     * GET /api/notifications/unread-count?userId=1
     * 
     * 응답 예시:
     * 3
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 알림 읽음 처리
     * 
     * 사용자가 알림 카드를 클릭하면 호출됩니다.
     * read = true로 변경되어 UI에서 "읽은 알림" 스타일로 표시됩니다.
     * 
     * @param id 알림 ID (Path Variable)
     * @param userId 현재 사용자 ID (쿼리 파라미터)
     * @return 성공 응답
     * 
     * 예시 요청:
     * PATCH /api/notifications/10/read?userId=1
     */
    /**
     * SSE 실시간 알림 스트림 연결
     * 
     * 클라이언트가 이 엔드포인트에 연결하면 서버에서 알림이 발생할 때마다
     * 실시간으로 이벤트를 전송합니다.
     * 
     * @param userId 현재 사용자 ID (쿼리 파라미터)
     * @return SseEmitter (SSE 연결)
     * 
     * 예시 요청:
     * GET /api/notifications/stream?userId=1
     * 
     * 프론트엔드 사용 예시 (JavaScript):
     * const eventSource = new EventSource('/api/notifications/stream?userId=1');
     * eventSource.addEventListener('notification', (event) => {
     *   const notification = JSON.parse(event.data);
     *   // 알림 UI 업데이트
     * });
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@RequestParam Long userId) {
        SseEmitter emitter = sseEmitters.create(userId);
        
        // 연결 즉시 초기 이벤트 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결이 성공적으로 설정되었습니다."));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * 알림 읽음 처리
     * 
     * 사용자가 알림 카드를 클릭하면 호출됩니다.
     * read = true로 변경되어 UI에서 "읽은 알림" 스타일로 표시됩니다.
     * 
     * @param id 알림 ID (Path Variable)
     * @param userId 현재 사용자 ID (쿼리 파라미터)
     * @return 성공 응답
     * 
     * 예시 요청:
     * PATCH /api/notifications/10/read?userId=1
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
}

