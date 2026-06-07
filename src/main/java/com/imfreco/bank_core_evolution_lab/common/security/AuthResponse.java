package com.imfreco.bank_core_evolution_lab.common.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Respuesta de autenticación JWT")
public record AuthResponse(
        @Schema(description = "JWT que debe enviarse en Authorization: Bearer <token>")
                String accessToken,
        @Schema(description = "Tipo de token", example = "Bearer") String tokenType,
        @Schema(description = "Tiempo de vida del token en segundos", example = "3600")
                long expiresInSeconds,
        @Schema(description = "Usuario autenticado", example = "customer") String username,
        @Schema(
                        description =
                                "Customer asociado cuando el usuario tiene rol CUSTOMER. Null para usuarios operativos.",
                        example = "11111111-1111-1111-1111-111111111111")
                UUID customerId,
        @Schema(description = "Roles asociados al usuario", example = "[\"CUSTOMER\"]")
                List<String> roles) {}
