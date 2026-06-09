package com.imfreco.bank_core_evolution_lab.account.application;

import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountResult;
import com.imfreco.bank_core_evolution_lab.account.application.port.in.AccountUseCase;
import com.imfreco.bank_core_evolution_lab.account.application.port.in.BalanceResult;
import com.imfreco.bank_core_evolution_lab.account.application.port.in.CreateAccountCommand;
import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.audit.application.port.out.AuditRecorderPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.BankMetricsPort;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.application.port.out.CustomerRepositoryPort;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventCreatorPort;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService implements AccountUseCase {

    private final AccountRepositoryPort accountRepository;
    private final CustomerRepositoryPort customerRepository;
    private final AuditRecorderPort auditRecorder;
    private final OutboxEventCreatorPort outboxEventCreator;
    private final BankMetricsPort bankMetrics;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(
            AccountRepositoryPort accountRepository,
            CustomerRepositoryPort customerRepository,
            AuditRecorderPort auditRecorder,
            OutboxEventCreatorPort outboxEventCreator,
            BankMetricsPort bankMetrics) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditRecorder = auditRecorder;
        this.outboxEventCreator = outboxEventCreator;
        this.bankMetrics = bankMetrics;
    }

    @Override
    @Transactional
    public AccountResult create(CreateAccountCommand command) {
        if (!customerRepository.existsById(command.customerId())) {
            throw new CustomerNotFoundException(command.customerId());
        }
        Account account =
                new Account(
                        generateAccountNumber(),
                        command.customerId(),
                        command.type(),
                        command.currency(),
                        command.initialBalance());
        return toResult(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResult get(UUID accountId) {
        return toResult(findAccount(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResult getBalance(UUID accountId) {
        return toBalanceResult(findAccount(accountId));
    }

    @Override
    @Transactional
    public AccountResult block(UUID accountId, String actor, String channel, String correlationId) {
        Account account = findAccount(accountId);
        boolean changed = account.block();
        if (changed) {
            auditRecorder.record(
                    "ACCOUNT_BLOCKED",
                    "Account",
                    account.getId().toString(),
                    actor,
                    channel,
                    correlationId,
                    Map.of(
                            "accountNumber",
                            account.getAccountNumber(),
                            "customerId",
                            account.getCustomerId()));
            outboxEventCreator.create(
                    "Account",
                    account.getId().toString(),
                    "AccountBlocked",
                    Map.of(
                            "accountId",
                            account.getId(),
                            "accountNumber",
                            account.getAccountNumber(),
                            "customerId",
                            account.getCustomerId(),
                            "correlationId",
                            correlationId));
            bankMetrics.incrementBlockedAccounts();
        }
        return toResult(account);
    }

    public Account findAccount(UUID accountId) {
        return accountRepository
                .findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
    }

    private String generateAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "10" + secureRandom.nextLong(1_000_000_000L, 9_999_999_999L);
            if (accountRepository.findByAccountNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique account number");
    }

    private AccountResult toResult(Account account) {
        return new AccountResult(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getType(),
                account.getStatus(),
                account.getCurrency(),
                account.getAccountingBalance(),
                account.getAvailableBalance(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }

    private BalanceResult toBalanceResult(Account account) {
        return new BalanceResult(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getAccountingBalance(),
                account.getAvailableBalance());
    }
}
