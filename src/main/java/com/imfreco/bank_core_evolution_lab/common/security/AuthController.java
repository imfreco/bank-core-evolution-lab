package com.imfreco.bank_core_evolution_lab.common.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login demo con JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login JWT",
            description =
                    "Recibe username/password demo y retorna un accessToken JWT. Luego pega ese token en el botón Authorize de Swagger.",
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = LoginRequest.class),
                                            examples =
                                                    @ExampleObject(
                                                            name = "Customer",
                                                            value =
                                                                    """
                                                                    {
                                                                      "username": "customer",
                                                                      "password": "customer123"
                                                                    }
                                                                    """))),
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Token JWT generado",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = AuthResponse.class))),
                @ApiResponse(responseCode = "400", description = "Request inválido"),
                @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
            },
            security = {})
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
