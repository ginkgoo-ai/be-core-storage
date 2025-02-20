package com.ginkgooai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: david
 * @date: 14:33 2025/2/20
 */

@Configuration
public class OpenAPIConfig {


    @Bean
    public OpenAPI customOpenAPI() {


        return new OpenAPI()
                .info(new Info()
                        .title("My API")
                        .version("1.0.0")
                        .description("API Documentation")
                );
    }
}
