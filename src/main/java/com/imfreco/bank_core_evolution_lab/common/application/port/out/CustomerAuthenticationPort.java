package com.imfreco.bank_core_evolution_lab.common.application.port.out;

import java.util.UUID;

public interface CustomerAuthenticationPort {

    boolean usernameExists(String username);

    void registerCustomerCredentials(String username, String rawPassword, UUID customerId);
}
