package taxi.tago.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 스웨거에서 참조할 Security Scheme 이름
        String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // 이 API는 bearerAuth라는 보안 스키마를 사용한다고 전체에 선언
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // 실제 Security Scheme 정의 (헤더에 Authorization: Bearer xxx 형태로 보내도록 설정)
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization")                 // 헤더 이름
                                        .type(SecurityScheme.Type.HTTP)       // HTTP auth 타입
                                        .scheme("bearer")                     // Bearer 스킴
                                        .bearerFormat("JWT")                  // 형식 설명 (옵션)
                                        .in(SecurityScheme.In.HEADER)         // 헤더에 넣는다는 의미
                        )
                )
                .info(new Info()
                        .title("슈슝 API 명세서")
                        .version("1.0")
                        .description("택시팟 서비스 슈슝의 REST API 명세서입니다.")
                        .contact(new Contact()
                                .name("슈슝")
                                .email("boyunyang@naver.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://swushoong.click")
                                .description("프로덕션 서버")
                ));
    }
}

