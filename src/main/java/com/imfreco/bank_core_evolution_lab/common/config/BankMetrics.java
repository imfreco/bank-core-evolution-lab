package com.imfreco.bank_core_evolution_lab.common.config;

import com.imfreco.bank_core_evolution_lab.common.application.port.out.BankMetricsPort;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventRepositoryPort;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BankMetrics implements BankMetricsPort {

    private final Counter successfulTransfers;
    private final Counter failedTransfers;
    private final Counter blockedAccounts;

    public BankMetrics(
            MeterRegistry meterRegistry, OutboxEventRepositoryPort outboxEventRepository) {
        this.successfulTransfers =
                Counter.builder("bank.transfers.successful")
                        .description("Successful internal transfers")
                        .register(meterRegistry);
        this.failedTransfers =
                Counter.builder("bank.transfers.failed")
                        .description("Failed internal transfers")
                        .register(meterRegistry);
        this.blockedAccounts =
                Counter.builder("bank.accounts.blocked")
                        .description("Preventive account blocks")
                        .register(meterRegistry);
        meterRegistry.gauge(
                "bank.outbox.events.pending",
                outboxEventRepository,
                repository -> repository.countByStatus(OutboxEventStatus.PENDING));
    }

    @Override
    public void incrementSuccessfulTransfers() {
        successfulTransfers.increment();
    }

    @Override
    public void incrementFailedTransfers() {
        failedTransfers.increment();
    }

    @Override
    public void incrementBlockedAccounts() {
        blockedAccounts.increment();
    }
}
