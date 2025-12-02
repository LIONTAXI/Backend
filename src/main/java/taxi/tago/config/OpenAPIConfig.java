package taxi.tago.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
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
                                .url("https://production-server.com")
                                .description("프로덕션 서버")
                ));
    }
}

