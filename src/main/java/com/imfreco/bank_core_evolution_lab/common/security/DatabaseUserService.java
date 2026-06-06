package com.imfreco.bank_core_evolution_lab.common.security;

import com.imfreco.bank_core_evolution_lab.common.exception.AuthenticationFailedException;
import java.util.ArrayList;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserService {

    private final AuthUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseUserService(AuthUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(String username, String password) {
        AuthUser user =
                repository
                        .findByUsername(username)
                        .filter(AuthUser::isEnabled)
                        .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        return new AuthenticatedUser(user.getUsername(), new ArrayList<>(user.getRoles()));
    }
}
