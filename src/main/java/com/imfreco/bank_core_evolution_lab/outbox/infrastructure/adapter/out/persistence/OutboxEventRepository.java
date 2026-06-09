package com.imfreco.bank_core_evolution_lab.outbox.infrastructure.adapter.out.persistence;

import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventRepositoryPort;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEvent;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID>, OutboxEventRepositoryPort {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    long countByStatus(OutboxEventStatus status);
}
