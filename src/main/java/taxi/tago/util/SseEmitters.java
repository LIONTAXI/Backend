package taxi.tago.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// SSE(Server-Sent Events) Emitter 관리 클래스 - 사용자별 SseEmitter를 관리하고, 알림 발생 시 해당 사용자에게 실시간으로 전송
@Slf4j
@Component
public class SseEmitters {

    // 사용자 ID별 SseEmitter 저장 (동시성 안전)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 타임아웃 (30분)
    private static final long TIMEOUT = 30 * 60 * 1000L;

    // 사용자별 SSE 연결 생성
    public SseEmitter create(Long userId) {
        // 기존 연결이 있으면 제거
        remove(userId);

        // 새로운 SseEmitter 생성
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);

        // 연결 종료 시 Map에서 제거
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE 연결 종료: userId={}", userId);
        });

        // 타임아웃 시 Map에서 제거
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("SSE 연결 타임아웃: userId={}", userId);
        });

        // 에러 발생 시 Map에서 제거
        emitter.onError((ex) -> {
            emitters.remove(userId);
            log.error("SSE 연결 에러: userId={}, error={}", userId, ex.getMessage());
        });

        log.info("SSE 연결 생성: userId={}, 현재 연결 수={}", userId, emitters.size());
        return emitter;
    }

    // 특정 사용자에게 알림 전송
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("SSE 알림 전송 성공: userId={}, eventName={}, 현재 연결 수={}", userId, eventName, emitters.size());
            } catch (IOException e) {
                // 전송 실패 시 연결 제거
                emitters.remove(userId);
                emitter.completeWithError(e);
                log.error("SSE 알림 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
            } catch (Exception e) {
                // 예상치 못한 오류
                emitters.remove(userId);
                emitter.completeWithError(e);
                log.error("SSE 알림 전송 중 예상치 못한 오류: userId={}, error={}", userId, e.getMessage(), e);
            }
        } else {
            log.warn("SSE 연결 없음 (알림 전송 스킵): userId={}, 현재 연결 수={}, 연결된 사용자={}", 
                    userId, emitters.size(), emitters.keySet());
        }
    }

    // 특정 사용자의 SSE 연결 제거
    public void remove(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 연결 제거: userId={}", userId);
        }
    }

    // 현재 연결된 사용자 수 조회
    public int getConnectionCount() {
        return emitters.size();
    }
}

