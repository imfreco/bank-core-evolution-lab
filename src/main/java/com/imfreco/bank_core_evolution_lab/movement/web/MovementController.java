package com.imfreco.bank_core_evolution_lab.movement.web;

import com.imfreco.bank_core_evolution_lab.movement.application.MovementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping("/accounts/{accountId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public List<MovementResponse> accountMovements(@PathVariable UUID accountId) {
        return movementService.findByAccount(accountId);
    }

    @GetMapping("/customers/{customerId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CUSTOMER')")
    public CustomerMovementViewResponse customerMovements(@PathVariable UUID customerId) {
        return movementService.findByCustomer(customerId);
    }
}
