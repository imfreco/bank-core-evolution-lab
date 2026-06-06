package com.imfreco.bank_core_evolution_lab.outbox.domain;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
