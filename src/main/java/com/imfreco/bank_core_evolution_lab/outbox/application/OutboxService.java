package com.imfreco.bank_core_evolution_lab.outbox.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventCreatorPort;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventRepositoryPort;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEvent;
import org.springframework.stereotype.Service;

@Service
public class OutboxService implements OutboxEventCreatorPort {

    private final OutboxEventRepositoryPort repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepositoryPort repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void create(String aggregateType, String aggregateId, String eventType, Object payload) {
        repository.save(new OutboxEvent(aggregateType, aggregateId, eventType, toJson(payload)));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox payload", exception);
        }
    }
}
