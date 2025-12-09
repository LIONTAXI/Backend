package taxi.tago.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import taxi.tago.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/login",
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll() // 로그인 및 인증 관련 엔드포인트, Swagger UI는 인증 불필요
                        .requestMatchers(HttpMethod.GET, "/api/taxi-party/*/requests").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/taxi-party/**").permitAll()
                        .requestMatchers(
                                "/api/taxi-party/**",
                                "/api/map/**",
                                "/api/users/**",
                                "/api/blocks/**",
                                "/api/chat/**",
                                "/api/settlements/**",
                                "/api/reviews/**"
                        ).authenticated() // 인증 필요
                        .anyRequest().permitAll() // 현재는 모든 요청 허용 (필요시 인증 필요로 변경 가능)
                );

        return http.build();
    }
}
