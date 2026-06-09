package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.movement.application.port.in.MovementUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MovementController {

    private final MovementUseCase movementUseCase;

    public MovementController(MovementUseCase movementUseCase) {
        this.movementUseCase = movementUseCase;
    }

    @GetMapping("/accounts/{accountId}/movements")
    @PreAuthorize("@authorizationService.canAccessAccount(#accountId)")
    public List<MovementResponse> accountMovements(@PathVariable UUID accountId) {
        return movementUseCase.findByAccount(accountId).stream()
                .map(MovementMapper::toResponse)
                .toList();
    }

    @GetMapping("/customers/{customerId}/movements")
    @PreAuthorize("@authorizationService.canAccessCustomer(#customerId)")
    public CustomerMovementViewResponse customerMovements(@PathVariable UUID customerId) {
        return MovementMapper.toResponse(movementUseCase.findByCustomer(customerId));
    }
}
