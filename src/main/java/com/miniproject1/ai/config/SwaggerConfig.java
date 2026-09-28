package com.miniproject1.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI aiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI API Document")
                        .description("지원사업 AI 검수 서버 API 명세서")
                        .version("v1.0.0"));
    }
}

