package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.movement.application.port.in.CustomerMovementViewResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.MovementResult;

public final class MovementMapper {

    private MovementMapper() {}

    public static MovementResponse toResponse(MovementResult movement) {
        return new MovementResponse(
                movement.id(),
                movement.accountId(),
                movement.transferReference(),
                movement.type(),
                movement.amount(),
                movement.currency(),
                movement.balanceAfterMovement(),
                movement.description(),
                movement.createdAt());
    }

    public static CustomerMovementViewResponse toResponse(CustomerMovementViewResult result) {
        return new CustomerMovementViewResponse(
                result.customerId(),
                result.movements().stream().map(MovementMapper::toResponse).toList(),
                result.updatedAt(),
                result.source());
    }
}
