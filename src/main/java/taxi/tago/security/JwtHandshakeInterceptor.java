package taxi.tago.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import taxi.tago.util.JwtUtil;

import java.security.Principal;
import java.util.Map;

// WebSocket 연결(handshake) 시점에 JWT를 검증하고 인증 정보(Principal)를 WebSocket 세션에 심어주는 인터셉터 클래스
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        try {
            String token = null;

            // Authorization 헤더 시도
            HttpHeaders headers = request.getHeaders();
            String bearerToken = headers.getFirst(HttpHeaders.AUTHORIZATION);
            token = resolveToken(bearerToken);

            // 헤더에 없으면 ?token=... 쿼리 파라미터에서 시도
            if (token == null && request instanceof ServletServerHttpRequest servletRequest) {
                String paramToken = servletRequest.getServletRequest().getParameter("token");
                if (StringUtils.hasText(paramToken)) {
                    token = paramToken;
                }
            }

            if (token != null && jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);

                // 기존 SecurityConfig에서 사용하던 UserDetailsService 재사용
                CustomUserDetails userDetails =
                        (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                // WebSocket 세션에 Principal 형태로 저장
                Principal principal = userDetails::getUsername;
                attributes.put("principal", principal);
                attributes.put("userDetails", userDetails);

                log.info("WebSocket Handshake 인증 성공: {}", email);
                // true를 리턴해야 handshake 계속 진행
                return true; // 정상 인증이면 통과
            } else {
                log.warn("WebSocket Handshake: JWT가 없거나 유효하지 않음");
                return false; // 인증 실패 시 연결 차단
            }
        } catch (Exception e) {
            log.error("WebSocket Handshake 중 JWT 처리 오류: {}", e.getMessage());
            return false; // 예외 상황도 차단
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 특별히 후처리할 내용 없음
    }

    private String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
