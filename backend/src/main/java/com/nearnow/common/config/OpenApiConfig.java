package com.nearnow.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Without this, springdoc has no idea our JwtAuthFilter reads a raw
 * "Authorization: Bearer <token>" header — it only knows about Spring
 * Security schemes it can introspect automatically (form login, basic
 * auth), and our filter is neither. This bean tells Swagger UI to render
 * the "Authorize" button and attach the header to every "Try it out" call
 * once a token is pasted in.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI nearNowOpenApi() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
