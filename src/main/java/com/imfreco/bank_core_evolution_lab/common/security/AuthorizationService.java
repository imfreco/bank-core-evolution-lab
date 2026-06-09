package com.imfreco.bank_core_evolution_lab.common.security;

import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authorizationService")
public class AuthorizationService {

    private final AccountRepositoryPort accountRepository;

    public AuthorizationService(AccountRepositoryPort accountRepository) {
        this.accountRepository = accountRepository;
    }

    public boolean canAccessCustomer(UUID customerId) {
        if (hasAnyRole("ROLE_ADMIN", "ROLE_OPERATOR")) {
            return true;
        }
        AuthenticatedUser user = currentUser();
        return user != null && customerId != null && customerId.equals(user.customerId());
    }

    public boolean canAccessAccount(UUID accountId) {
        if (hasAnyRole("ROLE_ADMIN", "ROLE_OPERATOR")) {
            return true;
        }
        AuthenticatedUser user = currentUser();
        if (user == null || user.customerId() == null || accountId == null) {
            return false;
        }
        return accountRepository
                .findById(accountId)
                .map(account -> user.customerId().equals(account.getCustomerId()))
                .orElse(false);
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    private boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (String role : roles) {
            boolean hasRole =
                    authentication.getAuthorities().stream()
                            .anyMatch(authority -> role.equals(authority.getAuthority()));
            if (hasRole) {
                return true;
            }
        }
        return false;
    }
}
