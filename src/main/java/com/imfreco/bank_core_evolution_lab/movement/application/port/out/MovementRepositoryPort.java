package com.imfreco.bank_core_evolution_lab.movement.application.port.out;

import com.imfreco.bank_core_evolution_lab.movement.domain.Movement;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MovementRepositoryPort {

    Movement save(Movement movement);

    List<Movement> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<Movement> findByAccountIdInOrderByCreatedAtDesc(Collection<UUID> accountIds);
}
