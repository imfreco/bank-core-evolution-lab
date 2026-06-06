package com.imfreco.bank_core_evolution_lab.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    OpenAPI bankCoreOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("bank-core-evolution-lab API")
                                .version("v1")
                                .description(
                                        "Laboratorio backend bancario con transferencias, idempotencia, auditoría y outbox pattern."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
