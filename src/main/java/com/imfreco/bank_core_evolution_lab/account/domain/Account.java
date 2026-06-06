package com.imfreco.bank_core_evolution_lab.account.domain;

import com.imfreco.bank_core_evolution_lab.common.exception.AccountBlockedException;
import com.imfreco.bank_core_evolution_lab.common.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "accounting_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal accountingBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public Account(String accountNumber, UUID customerId, AccountType type, Currency currency, BigDecimal initialBalance) {
        this.id = UUID.randomUUID();
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.type = type;
        this.status = AccountStatus.ACTIVE;
        this.currency = currency;
        this.accountingBalance = initialBalance;
        this.availableBalance = initialBalance;
        this.version = 0L;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (version == null) {
            this.version = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        ensureActive();
        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        this.accountingBalance = this.accountingBalance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        ensureActive();
        this.accountingBalance = this.accountingBalance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public boolean block() {
        if (status == AccountStatus.BLOCKED) {
            return false;
        }
        ensureActive();
        this.status = AccountStatus.BLOCKED;
        return true;
    }

    public void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountBlockedException(accountNumber);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public AccountType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getAccountingBalance() {
        return accountingBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
