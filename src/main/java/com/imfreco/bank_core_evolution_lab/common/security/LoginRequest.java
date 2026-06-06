package com.imfreco.bank_core_evolution_lab.common.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales demo para obtener un JWT")
public record LoginRequest(
        @Schema(description = "Usuario demo", example = "customer") @NotBlank String username,
        @Schema(description = "Password demo", example = "customer123") @NotBlank
                String password) {}
