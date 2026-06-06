package com.imfreco.bank_core_evolution_lab.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "operation_type", nullable = false, length = 80)
    private String operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String requestHash, String operationType) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.operationType = operationType;
        this.status = IdempotencyStatus.IN_PROGRESS;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public void complete(String responseBody) {
        this.responseBody = responseBody;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getOperationType() {
        return operationType;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
