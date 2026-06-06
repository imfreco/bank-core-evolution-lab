package com.imfreco.bank_core_evolution_lab.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "operation_type", nullable = false, length = 80)
    private String operationType;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 80)
    private String entityId;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 40)
    private String channel;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(nullable = false, columnDefinition = "text")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(String operationType, String entityType, String entityId, String actor,
                    String channel, String correlationId, String details) {
        this.id = UUID.randomUUID();
        this.operationType = operationType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actor = actor;
        this.channel = channel;
        this.correlationId = correlationId;
        this.details = details;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getActor() {
        return actor;
    }

    public String getChannel() {
        return channel;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
