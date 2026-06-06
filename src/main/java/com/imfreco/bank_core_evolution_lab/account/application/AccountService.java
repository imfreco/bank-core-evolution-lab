package com.imfreco.bank_core_evolution_lab.account.application;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.account.web.AccountMapper;
import com.imfreco.bank_core_evolution_lab.account.web.AccountRequest;
import com.imfreco.bank_core_evolution_lab.account.web.AccountResponse;
import com.imfreco.bank_core_evolution_lab.account.web.BalanceResponse;
import com.imfreco.bank_core_evolution_lab.audit.application.AuditService;
import com.imfreco.bank_core_evolution_lab.common.config.BankMetrics;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.infrastructure.CustomerRepository;
import com.imfreco.bank_core_evolution_lab.outbox.application.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final BankMetrics bankMetrics;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository,
                          AuditService auditService, OutboxService outboxService, BankMetrics bankMetrics) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.bankMetrics = bankMetrics;
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        if (!customerRepository.existsById(request.customerId())) {
            throw new CustomerNotFoundException(request.customerId());
        }
        Account account = new Account(
                generateAccountNumber(),
                request.customerId(),
                request.type(),
                request.currency(),
                request.initialBalance()
        );
        return AccountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID accountId) {
        return AccountMapper.toResponse(findAccount(accountId));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        return AccountMapper.toBalanceResponse(findAccount(accountId));
    }

    @Transactional
    public AccountResponse block(UUID accountId, String actor, String channel, String correlationId) {
        Account account = findAccount(accountId);
        boolean changed = account.block();
        if (changed) {
            auditService.record("ACCOUNT_BLOCKED", "Account", account.getId().toString(), actor, channel, correlationId,
                    Map.of("accountNumber", account.getAccountNumber(), "customerId", account.getCustomerId()));
            outboxService.create("Account", account.getId().toString(), "AccountBlocked",
                    Map.of("accountId", account.getId(), "accountNumber", account.getAccountNumber(),
                            "customerId", account.getCustomerId(), "correlationId", correlationId));
            bankMetrics.incrementBlockedAccounts();
        }
        return AccountMapper.toResponse(account);
    }

    public Account findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
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
}
