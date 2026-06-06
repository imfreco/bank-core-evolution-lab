package com.imfreco.bank_core_evolution_lab.transfer.web;

import com.imfreco.bank_core_evolution_lab.common.api.ActorContext;
import com.imfreco.bank_core_evolution_lab.common.logging.CorrelationIdFilter;
import com.imfreco.bank_core_evolution_lab.transfer.application.TransferService;
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

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public TransferResponse create(@Valid @RequestBody TransferRequest request,
                                   @RequestHeader("Idempotency-Key") String idempotencyKey,
                                   @RequestHeader(name = "X-Channel", required = false) String channel,
                                   Principal principal) {
        return transferService.create(
                request,
                idempotencyKey,
                ActorContext.actor(principal),
                ActorContext.channel(channel),
                CorrelationIdFilter.currentCorrelationId()
        );
    }

    @GetMapping("/{transferReference}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public TransferResponse get(@PathVariable String transferReference) {
        return transferService.findByReference(transferReference);
    }
}
