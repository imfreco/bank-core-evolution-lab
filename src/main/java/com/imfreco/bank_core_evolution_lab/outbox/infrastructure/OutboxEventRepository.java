package com.imfreco.bank_core_evolution_lab.outbox.infrastructure;

import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEvent;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    long countByStatus(OutboxEventStatus status);
}
