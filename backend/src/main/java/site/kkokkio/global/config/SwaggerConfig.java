package site.kkokkio.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // Swagger info 작성 (제목, 버전, 설명)
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("꼬끼오 API")
                .version("Ver 1.0")
                .description("프로젝트 '꼬끼오' 서비스의 백엔드 API 명세서입니다.");

        return new OpenAPI()
                .info(info);
    }
}