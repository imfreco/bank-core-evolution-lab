package com.imfreco.bank_core_evolution_lab.movement.application.port.out;

import com.imfreco.bank_core_evolution_lab.movement.application.port.in.CustomerMovementViewResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.MovementResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementProjectionPort {

    Optional<CustomerMovementViewResult> findView(UUID customerId);

    void refreshCustomerMovements(UUID customerId, List<MovementResult> sqlMovements);
}
