package com.imfreco.bank_core_evolution_lab.transfer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.common.api.ActorContext;
import com.imfreco.bank_core_evolution_lab.common.application.security.AuthenticatedActor;
import com.imfreco.bank_core_evolution_lab.common.logging.CorrelationIdFilter;
import com.imfreco.bank_core_evolution_lab.common.security.AuthenticatedUser;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.TransferUseCase;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferUseCase transferUseCase;

    public TransferController(TransferUseCase transferUseCase) {
        this.transferUseCase = transferUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public TransferResponse create(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Channel", required = false) String channel,
            Principal principal) {
        return TransferMapper.toResponse(
                transferUseCase.create(
                        TransferMapper.toCommand(
                                request,
                                idempotencyKey,
                                toAuthenticatedActor(principal),
                                ActorContext.channel(channel),
                                CorrelationIdFilter.currentCorrelationId())));
    }

    @GetMapping("/{transferReference}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public TransferResponse get(@PathVariable String transferReference, Principal principal) {
        return TransferMapper.toResponse(
                transferUseCase.findByReference(
                        transferReference, toAuthenticatedActor(principal)));
    }

    private AuthenticatedActor toAuthenticatedActor(Principal principal) {
        if (principal instanceof AuthenticatedUser user) {
            return new AuthenticatedActor(user.username(), user.roles(), user.customerId());
        }
        if (principal instanceof Authentication authentication) {
            if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
                return new AuthenticatedActor(user.username(), user.roles(), user.customerId());
            }
            return new AuthenticatedActor(
                    authentication.getName(),
                    authentication.getAuthorities().stream()
                            .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                            .toList(),
                    null);
        }
        return new AuthenticatedActor(ActorContext.actor(principal), List.of(), null);
    }
}
