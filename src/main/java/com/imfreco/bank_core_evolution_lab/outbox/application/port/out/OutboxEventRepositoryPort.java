package com.imfreco.bank_core_evolution_lab.outbox.application.port.out;

import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEvent;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import java.util.List;

public interface OutboxEventRepositoryPort {

    OutboxEvent save(OutboxEvent outboxEvent);

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    long countByStatus(OutboxEventStatus status);
}
