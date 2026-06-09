package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.out.persistence;

import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementRepositoryPort;
import com.imfreco.bank_core_evolution_lab.movement.domain.Movement;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, UUID>, MovementRepositoryPort {

    List<Movement> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<Movement> findByAccountIdInOrderByCreatedAtDesc(Collection<UUID> accountIds);
}
