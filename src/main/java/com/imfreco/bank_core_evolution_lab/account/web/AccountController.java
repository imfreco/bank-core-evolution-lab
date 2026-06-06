package com.imfreco.bank_core_evolution_lab.account.web;

import com.imfreco.bank_core_evolution_lab.account.application.AccountService;
import com.imfreco.bank_core_evolution_lab.common.api.ActorContext;
import com.imfreco.bank_core_evolution_lab.common.logging.CorrelationIdFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public AccountResponse get(@PathVariable UUID accountId) {
        return accountService.get(accountId);
    }

    @GetMapping("/{accountId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public BalanceResponse balance(@PathVariable UUID accountId) {
        return accountService.getBalance(accountId);
    }

    @PostMapping("/{accountId}/block")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse block(@PathVariable UUID accountId,
                                 @RequestHeader(name = "X-Channel", required = false) String channel,
                                 Principal principal) {
        return accountService.block(
                accountId,
                ActorContext.actor(principal),
                ActorContext.channel(channel),
                CorrelationIdFilter.currentCorrelationId()
        );
    }
}
