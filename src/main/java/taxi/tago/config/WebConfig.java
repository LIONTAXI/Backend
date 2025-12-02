package taxi.tago.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "/images/**" 주소로 요청이 오면 프로젝트 폴더 내의 "src/main/resources/static/images/" 경로에서 탐색
        String projectPath = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\images\\";

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + projectPath);
    }
}