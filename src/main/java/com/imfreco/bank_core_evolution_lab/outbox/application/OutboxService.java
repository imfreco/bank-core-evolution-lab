package com.imfreco.bank_core_evolution_lab.outbox.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEvent;
import com.imfreco.bank_core_evolution_lab.outbox.infrastructure.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

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
