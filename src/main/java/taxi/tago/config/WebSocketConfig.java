package taxi.tago.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import taxi.tago.security.JwtHandshakeInterceptor;

// STOMP 기반 WebSocket 설정 클래스
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // WebSocket 연결 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 허용 (실서비스에선 도메인 허용으로 변경)
                .addInterceptors(jwtHandshakeInterceptor) // handshake 시 JWT 검사 & Principal 설정
                .withSockJS(); // WebSocket 미지원 브라우저를 위한 SockJS fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트 방향 (브로커가 관리하는 구독 경로 prefix)
        registry.enableSimpleBroker("/topic");

        // 클라이언트 -> 서버 방향 (@MessageMapping 메서드로 라우팅될 prefix)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
