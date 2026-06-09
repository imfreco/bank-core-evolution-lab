package com.imfreco.bank_core_evolution_lab.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountResult;
import com.imfreco.bank_core_evolution_lab.account.application.port.in.CreateAccountCommand;
import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountStatus;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.audit.application.port.out.AuditRecorderPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.BankMetricsPort;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.application.port.out.CustomerRepositoryPort;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventCreatorPort;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final CustomerRepositoryPort customerRepository = mock(CustomerRepositoryPort.class);
    private final AuditRecorderPort auditService = mock(AuditRecorderPort.class);
    private final OutboxEventCreatorPort outboxService = mock(OutboxEventCreatorPort.class);
    private final BankMetricsPort bankMetrics = mock(BankMetricsPort.class);
    private final AccountService service =
            new AccountService(
                    accountRepository,
                    customerRepository,
                    auditService,
                    outboxService,
                    bankMetrics);

    @Test
    void blocksActiveAccountAndRecordsOperationalSignals() {
        UUID accountId = UUID.randomUUID();
        Account account =
                new Account(
                        "1000000001",
                        UUID.randomUUID(),
                        AccountType.SAVINGS,
                        Currency.COP,
                        new BigDecimal("100000.00"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountResult response = service.block(accountId, "operator", "WEB", "corr-1");

        assertThat(response.status()).isEqualTo(AccountStatus.BLOCKED);
        verify(auditService)
                .record(
                        eq("ACCOUNT_BLOCKED"),
                        eq("Account"),
                        eq(account.getId().toString()),
                        eq("operator"),
                        eq("WEB"),
                        eq("corr-1"),
                        any());
        verify(outboxService)
                .create(eq("Account"), eq(account.getId().toString()), eq("AccountBlocked"), any());
        verify(bankMetrics).incrementBlockedAccounts();
    }

    @Test
    void createRejectsUnknownCustomer() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateAccountCommand(
                                                customerId,
                                                AccountType.SAVINGS,
                                                Currency.COP,
                                                BigDecimal.ZERO)))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
