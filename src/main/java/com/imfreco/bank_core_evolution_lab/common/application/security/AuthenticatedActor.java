package com.imfreco.bank_core_evolution_lab.common.application.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedActor(String username, List<String> roles, UUID customerId) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdministrative() {
        return hasRole("ADMIN") || hasRole("OPERATOR");
    }
}
