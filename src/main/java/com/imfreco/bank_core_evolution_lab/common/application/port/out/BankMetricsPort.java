package com.imfreco.bank_core_evolution_lab.common.application.port.out;

public interface BankMetricsPort {

    void incrementSuccessfulTransfers();

    void incrementFailedTransfers();

    void incrementBlockedAccounts();
}
