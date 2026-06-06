package com.imfreco.bank_core_evolution_lab.transfer.application;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.audit.application.AuditService;
import com.imfreco.bank_core_evolution_lab.common.config.BankMetrics;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CurrencyMismatchException;
import com.imfreco.bank_core_evolution_lab.common.exception.DomainException;
import com.imfreco.bank_core_evolution_lab.common.exception.InvalidTransferException;
import com.imfreco.bank_core_evolution_lab.common.exception.TransferNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.idempotency.IdempotencyService;
import com.imfreco.bank_core_evolution_lab.movement.application.MovementService;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import com.imfreco.bank_core_evolution_lab.outbox.application.OutboxService;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import com.imfreco.bank_core_evolution_lab.transfer.infrastructure.TransferRepository;
import com.imfreco.bank_core_evolution_lab.transfer.web.TransferRequest;
import com.imfreco.bank_core_evolution_lab.transfer.web.TransferResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private static final String OPERATION_TYPE = "INTERNAL_TRANSFER";
    private static final String DESCRIPTION = "Transferencia interna";

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final MovementService movementService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final BankMetrics bankMetrics;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository,
                           MovementService movementService, IdempotencyService idempotencyService,
                           AuditService auditService, OutboxService outboxService, BankMetrics bankMetrics) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.movementService = movementService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.bankMetrics = bankMetrics;
    }

    @Transactional
    public TransferResponse create(TransferRequest request, String idempotencyKey,
                                   String actor, String channel, String correlationId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException("Idempotency-Key header is required");
        }

        return idempotencyService.findCompletedResponseOrCreateRecord(
                        idempotencyKey.trim(),
                        request,
                        OPERATION_TYPE,
                        TransferResponse.class
                )
                .orElseGet(() -> executeNewTransfer(request, idempotencyKey.trim(), actor, channel, correlationId));
    }

    @Transactional(readOnly = true)
    public TransferResponse findByReference(String transferReference) {
        Transfer transfer = transferRepository.findByTransferReference(transferReference)
                .orElseThrow(() -> new TransferNotFoundException(transferReference));
        Account source = accountRepository.findById(transfer.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(transfer.getSourceAccountId().toString()));
        Account target = accountRepository.findById(transfer.getTargetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(transfer.getTargetAccountId().toString()));
        return toResponse(transfer, source, target);
    }

    private TransferResponse executeNewTransfer(TransferRequest request, String idempotencyKey,
                                                String actor, String channel, String correlationId) {
        try {
            TransferResponse response = transferAtomically(request, idempotencyKey, actor, channel, correlationId);
            idempotencyService.complete(idempotencyKey, response);
            bankMetrics.incrementSuccessfulTransfers();
            return response;
        } catch (DomainException exception) {
            bankMetrics.incrementFailedTransfers();
            throw exception;
        } catch (RuntimeException exception) {
            bankMetrics.incrementFailedTransfers();
            throw exception;
        }
    }

    private TransferResponse transferAtomically(TransferRequest request, String idempotencyKey,
                                                String actor, String channel, String correlationId) {
        if (request.sourceAccountNumber().equals(request.targetAccountNumber())) {
            throw new InvalidTransferException("Source and target account must be different");
        }

        List<Account> lockedAccounts = accountRepository.findAllByAccountNumberInForUpdate(
                List.of(request.sourceAccountNumber(), request.targetAccountNumber()));
        Map<String, Account> accountsByNumber = lockedAccounts.stream()
                .collect(Collectors.toMap(Account::getAccountNumber, Function.identity()));

        Account source = accountOrThrow(accountsByNumber, request.sourceAccountNumber());
        Account target = accountOrThrow(accountsByNumber, request.targetAccountNumber());

        source.ensureActive();
        target.ensureActive();
        if (source.getCurrency() != target.getCurrency() || source.getCurrency() != request.currency()) {
            throw new CurrencyMismatchException();
        }

        Transfer transfer = transferRepository.save(new Transfer(
                source.getId(),
                target.getId(),
                request.amount(),
                request.currency(),
                idempotencyKey
        ));

        source.debit(request.amount());
        target.credit(request.amount());

        movementService.create(source, transfer.getId(), MovementType.DEBIT, request.amount(), DESCRIPTION);
        movementService.create(target, transfer.getId(), MovementType.CREDIT, request.amount(), DESCRIPTION);

        transfer.complete();

        auditService.record("TRANSFER_COMPLETED", "Transfer", transfer.getTransferReference(), actor, channel, correlationId,
                Map.of("sourceAccountId", source.getId(), "targetAccountId", target.getId(),
                        "amount", request.amount(), "currency", request.currency()));

        outboxService.create("Transfer", transfer.getTransferReference(), "TransferCompleted",
                Map.of("transferReference", transfer.getTransferReference(),
                        "sourceAccountId", source.getId(),
                        "targetAccountId", target.getId(),
                        "amount", request.amount(),
                        "currency", request.currency(),
                        "createdAt", transfer.getCreatedAt(),
                        "correlationId", correlationId));

        return toResponse(transfer, source, target);
    }

    private Account accountOrThrow(Map<String, Account> accountsByNumber, String accountNumber) {
        Account account = accountsByNumber.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    private TransferResponse toResponse(Transfer transfer, Account source, Account target) {
        return new TransferResponse(
                transfer.getTransferReference(),
                transfer.getSourceAccountId(),
                source.getAccountNumber(),
                transfer.getTargetAccountId(),
                target.getAccountNumber(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}
