package com.imfreco.bank_core_evolution_lab.movement.infrastructure;

import com.imfreco.bank_core_evolution_lab.movement.domain.Movement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    List<Movement> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<Movement> findByAccountIdInOrderByCreatedAtDesc(Collection<UUID> accountIds);
}
