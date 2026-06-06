package com.imfreco.bank_core_evolution_lab.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountStatus;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.account.web.AccountResponse;
import com.imfreco.bank_core_evolution_lab.audit.application.AuditService;
import com.imfreco.bank_core_evolution_lab.common.config.BankMetrics;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.infrastructure.CustomerRepository;
import com.imfreco.bank_core_evolution_lab.outbox.application.OutboxService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final OutboxService outboxService = mock(OutboxService.class);
    private final BankMetrics bankMetrics = mock(BankMetrics.class);
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

        AccountResponse response = service.block(accountId, "operator", "WEB", "corr-1");

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
                                        new com.imfreco.bank_core_evolution_lab.account.web
                                                .AccountRequest(
                                                customerId,
                                                AccountType.SAVINGS,
                                                Currency.COP,
                                                BigDecimal.ZERO)))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}
