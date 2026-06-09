package com.imfreco.bank_core_evolution_lab.movement.application.port.in;

import java.util.List;
import java.util.UUID;

public interface MovementUseCase {

    List<MovementResult> findByAccount(UUID accountId);

    CustomerMovementViewResult findByCustomer(UUID customerId);
}
