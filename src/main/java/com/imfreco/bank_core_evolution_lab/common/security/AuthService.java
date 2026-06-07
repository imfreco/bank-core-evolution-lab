package com.imfreco.bank_core_evolution_lab.common.security;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final DatabaseUserService databaseUserService;
    private final JwtService jwtService;

    public AuthService(DatabaseUserService databaseUserService, JwtService jwtService) {
        this.databaseUserService = databaseUserService;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        AuthenticatedUser user =
                databaseUserService.authenticate(request.username(), request.password());
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.expirationSeconds(),
                user.username(),
                user.customerId(),
                user.roles());
    }
}
