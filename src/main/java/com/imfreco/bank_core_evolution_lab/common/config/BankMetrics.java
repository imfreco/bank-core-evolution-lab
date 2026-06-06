package com.imfreco.bank_core_evolution_lab.common.config;

import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import com.imfreco.bank_core_evolution_lab.outbox.infrastructure.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BankMetrics {

    private final Counter successfulTransfers;
    private final Counter failedTransfers;
    private final Counter blockedAccounts;

    public BankMetrics(MeterRegistry meterRegistry, OutboxEventRepository outboxEventRepository) {
        this.successfulTransfers = Counter.builder("bank.transfers.successful")
                .description("Successful internal transfers")
                .register(meterRegistry);
        this.failedTransfers = Counter.builder("bank.transfers.failed")
                .description("Failed internal transfers")
                .register(meterRegistry);
        this.blockedAccounts = Counter.builder("bank.accounts.blocked")
                .description("Preventive account blocks")
                .register(meterRegistry);
        meterRegistry.gauge("bank.outbox.events.pending", outboxEventRepository,
                repository -> repository.countByStatus(OutboxEventStatus.PENDING));
    }

    public void incrementSuccessfulTransfers() {
        successfulTransfers.increment();
    }

    public void incrementFailedTransfers() {
        failedTransfers.increment();
    }

    public void incrementBlockedAccounts() {
        blockedAccounts.increment();
    }
}
