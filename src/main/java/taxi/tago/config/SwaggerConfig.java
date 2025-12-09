package taxi.tago.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 보안 스키마 설정 (JWT 설정)
        String jwt = "JWT";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);

        // Components 설정
        Components components = new Components().addSecuritySchemes(jwt, new SecurityScheme()
                .name(jwt)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
        );

        return new OpenAPI()
                .components(components)
                .info(apiInfo())
                .addSecurityItem(securityRequirement)
                .servers(List.of(
                        new Server().url("https://swushoong.click").description("배포 서버 (HTTPS)"),
                        new Server().url("http://localhost:8080").description("로컬 서버")
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("슈슝 API 명세서")
                .description("택시팟 서비스 슈슝의 REST API 명세서입니다.")
                .version("1.0.0");
    }
}