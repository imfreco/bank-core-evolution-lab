package com.imfreco.bank_core_evolution_lab.common.security;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(String username, List<String> roles, UUID customerId)
        implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
