package com.imfreco.bank_core_evolution_lab.account.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountUseCase;
import com.imfreco.bank_core_evolution_lab.common.api.ActorContext;
import com.imfreco.bank_core_evolution_lab.common.logging.CorrelationIdFilter;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
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

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountUseCase accountUseCase;

    public AccountController(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return AccountMapper.toResponse(accountUseCase.create(AccountMapper.toCommand(request)));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("@authorizationService.canAccessAccount(#accountId)")
    public AccountResponse get(@PathVariable UUID accountId) {
        return AccountMapper.toResponse(accountUseCase.get(accountId));
    }

    @GetMapping("/{accountId}/balance")
    @PreAuthorize("@authorizationService.canAccessAccount(#accountId)")
    public BalanceResponse balance(@PathVariable UUID accountId) {
        return AccountMapper.toBalanceResponse(accountUseCase.getBalance(accountId));
    }

    @PostMapping("/{accountId}/block")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public AccountResponse block(
            @PathVariable UUID accountId,
            @RequestHeader(name = "X-Channel", required = false) String channel,
            Principal principal) {
        return AccountMapper.toResponse(
                accountUseCase.block(
                        accountId,
                        ActorContext.actor(principal),
                        ActorContext.channel(channel),
                        CorrelationIdFilter.currentCorrelationId()));
    }
}
