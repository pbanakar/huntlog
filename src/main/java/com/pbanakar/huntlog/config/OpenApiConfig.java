package com.pbanakar.huntlog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI huntLogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HuntLog API")
                        .description("Job Application Tracker API — " +
                                "track applications, enforce lifecycle " +
                                "transitions, and get analytics on your " +
                                "job search progress.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Prajwal Banakar")
                                .url("https://prajwal-banakar.me")
                                .email("prajwal.banakara@gmail.com")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. " +
                                                "Get it from POST /api/v1/auth/login")));
    }
}
