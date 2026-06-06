package com.imfreco.bank_core_evolution_lab.movement.domain;

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
@Table(name = "movements")
public class Movement {

    @Id private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "balance_after_movement", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfterMovement;

    @Column(nullable = false, length = 250)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Movement() {}

    public Movement(
            UUID accountId,
            UUID transferId,
            MovementType type,
            BigDecimal amount,
            Currency currency,
            BigDecimal balanceAfterMovement,
            String description) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.transferId = transferId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfterMovement = balanceAfterMovement;
        this.description = description;
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

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public MovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalanceAfterMovement() {
        return balanceAfterMovement;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
