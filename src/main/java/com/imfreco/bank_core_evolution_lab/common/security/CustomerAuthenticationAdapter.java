package com.imfreco.bank_core_evolution_lab.common.security;

import com.imfreco.bank_core_evolution_lab.common.application.port.out.CustomerAuthenticationPort;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomerAuthenticationAdapter implements CustomerAuthenticationPort {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAuthenticationAdapter(
            AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean usernameExists(String username) {
        return authUserRepository.findByUsername(username).isPresent();
    }

    @Override
    public void registerCustomerCredentials(String username, String rawPassword, UUID customerId) {
        authUserRepository.save(
                new AuthUser(
                        username,
                        passwordEncoder.encode(rawPassword),
                        customerId,
                        true,
                        List.of("CUSTOMER")));
    }
}
