package taxi.tago.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API 경로에 대해
                .allowedOrigins("http://localhost:3000") // 프론트엔드 주소 (React 기본값 3000, 다르면 수정 필요)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true); // 쿠키/인증 정보 포함 허용
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "/images/**" 주소로 요청이 오면 프로젝트 폴더 내의 "src/main/resources/static/images/" 경로에서 탐색
        String projectPath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\images\\";

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + projectPath);
    }
}