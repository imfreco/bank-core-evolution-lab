package com.imfreco.bank_core_evolution_lab.outbox.application.port.out;

public interface OutboxEventCreatorPort {

    void create(String aggregateType, String aggregateId, String eventType, Object payload);
}
