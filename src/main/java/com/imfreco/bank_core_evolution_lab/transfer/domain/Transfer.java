package com.imfreco.bank_core_evolution_lab.transfer.domain;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id private UUID id;

    @Column(name = "transfer_reference", nullable = false, unique = true, length = 64)
    private String transferReference;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false)
    private UUID targetAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Transfer() {}

    public Transfer(
            UUID sourceAccountId,
            UUID targetAccountId,
            BigDecimal amount,
            Currency currency,
            String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.transferReference = UUID.randomUUID().toString();
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = TransferStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public void complete() {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
