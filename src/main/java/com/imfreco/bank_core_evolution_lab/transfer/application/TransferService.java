package com.imfreco.bank_core_evolution_lab.transfer.application;

import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.audit.application.port.out.AuditRecorderPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.BankMetricsPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.IdempotencyPort;
import com.imfreco.bank_core_evolution_lab.common.application.security.AuthenticatedActor;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CurrencyMismatchException;
import com.imfreco.bank_core_evolution_lab.common.exception.DomainException;
import com.imfreco.bank_core_evolution_lab.common.exception.ForbiddenOperationException;
import com.imfreco.bank_core_evolution_lab.common.exception.InvalidTransferException;
import com.imfreco.bank_core_evolution_lab.common.exception.TransferNotFoundException;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementRecorderPort;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventCreatorPort;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.CreateTransferCommand;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.TransferResult;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.TransferUseCase;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.out.TransferRepositoryPort;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService implements TransferUseCase {

    private static final String OPERATION_TYPE = "INTERNAL_TRANSFER";
    private static final String DESCRIPTION = "Transferencia interna";

    private final AccountRepositoryPort accountRepository;
    private final TransferRepositoryPort transferRepository;
    private final MovementRecorderPort movementRecorder;
    private final IdempotencyPort idempotency;
    private final AuditRecorderPort auditRecorder;
    private final OutboxEventCreatorPort outboxEventCreator;
    private final BankMetricsPort bankMetrics;

    public TransferService(
            AccountRepositoryPort accountRepository,
            TransferRepositoryPort transferRepository,
            MovementRecorderPort movementRecorder,
            IdempotencyPort idempotency,
            AuditRecorderPort auditRecorder,
            OutboxEventCreatorPort outboxEventCreator,
            BankMetricsPort bankMetrics) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.movementRecorder = movementRecorder;
        this.idempotency = idempotency;
        this.auditRecorder = auditRecorder;
        this.outboxEventCreator = outboxEventCreator;
        this.bankMetrics = bankMetrics;
    }

    @Override
    @Transactional
    public TransferResult create(CreateTransferCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new InvalidTransferException("Idempotency-Key header is required");
        }
        validateSourceAccountOwnershipBeforeIdempotency(
                command.sourceAccountNumber(), command.actor());

        return idempotency
                .findCompletedResponseOrCreateRecord(
                        command.idempotencyKey().trim(),
                        idempotencyPayload(command),
                        OPERATION_TYPE,
                        TransferResult.class)
                .orElseGet(() -> executeNewTransfer(command, command.idempotencyKey().trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResult findByReference(String transferReference, AuthenticatedActor actor) {
        Transfer transfer =
                transferRepository
                        .findByTransferReference(transferReference)
                        .orElseThrow(() -> new TransferNotFoundException(transferReference));
        Account source =
                accountRepository
                        .findById(transfer.getSourceAccountId())
                        .orElseThrow(
                                () ->
                                        new AccountNotFoundException(
                                                transfer.getSourceAccountId().toString()));
        Account target =
                accountRepository
                        .findById(transfer.getTargetAccountId())
                        .orElseThrow(
                                () ->
                                        new AccountNotFoundException(
                                                transfer.getTargetAccountId().toString()));
        ensureTransferCanBeViewedByCurrentActor(source, target, actor);
        return toResponse(transfer, source, target);
    }

    private TransferResult executeNewTransfer(
            CreateTransferCommand command, String idempotencyKey) {
        try {
            TransferResult response = transferAtomically(command, idempotencyKey);
            idempotency.complete(idempotencyKey, response);
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

    private TransferResult transferAtomically(
            CreateTransferCommand command, String idempotencyKey) {
        if (command.sourceAccountNumber().equals(command.targetAccountNumber())) {
            throw new InvalidTransferException("Source and target account must be different");
        }

        List<Account> lockedAccounts =
                accountRepository.findAllByAccountNumberInForUpdate(
                        List.of(command.sourceAccountNumber(), command.targetAccountNumber()));
        Map<String, Account> accountsByNumber =
                lockedAccounts.stream()
                        .collect(Collectors.toMap(Account::getAccountNumber, Function.identity()));

        Account source = accountOrThrow(accountsByNumber, command.sourceAccountNumber());
        Account target = accountOrThrow(accountsByNumber, command.targetAccountNumber());

        ensureSourceAccountCanBeDebitedByCurrentActor(source, command.actor());
        source.ensureActive();
        target.ensureActive();
        if (source.getCurrency() != target.getCurrency()
                || source.getCurrency() != command.currency()) {
            throw new CurrencyMismatchException();
        }

        Transfer transfer =
                transferRepository.save(
                        new Transfer(
                                source.getId(),
                                target.getId(),
                                command.amount(),
                                command.currency(),
                                idempotencyKey));

        source.debit(command.amount());
        target.credit(command.amount());

        movementRecorder.create(
                source, transfer.getId(), MovementType.DEBIT, command.amount(), DESCRIPTION);
        movementRecorder.create(
                target, transfer.getId(), MovementType.CREDIT, command.amount(), DESCRIPTION);

        transfer.complete();

        auditRecorder.record(
                "TRANSFER_COMPLETED",
                "Transfer",
                transfer.getTransferReference(),
                command.actor().username(),
                command.channel(),
                command.correlationId(),
                Map.of(
                        "sourceAccountId",
                        source.getId(),
                        "targetAccountId",
                        target.getId(),
                        "amount",
                        command.amount(),
                        "currency",
                        command.currency()));

        outboxEventCreator.create(
                "Transfer",
                transfer.getTransferReference(),
                "TransferCompleted",
                Map.of(
                        "transferReference",
                        transfer.getTransferReference(),
                        "sourceAccountId",
                        source.getId(),
                        "targetAccountId",
                        target.getId(),
                        "amount",
                        command.amount(),
                        "currency",
                        command.currency(),
                        "createdAt",
                        transfer.getCreatedAt(),
                        "correlationId",
                        command.correlationId()));

        return toResponse(transfer, source, target);
    }

    private Account accountOrThrow(Map<String, Account> accountsByNumber, String accountNumber) {
        Account account = accountsByNumber.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    private void validateSourceAccountOwnershipBeforeIdempotency(
            String sourceAccountNumber, AuthenticatedActor actor) {
        try {
            Account source =
                    accountRepository
                            .findByAccountNumber(sourceAccountNumber)
                            .orElseThrow(() -> new AccountNotFoundException(sourceAccountNumber));
            ensureSourceAccountCanBeDebitedByCurrentActor(source, actor);
        } catch (DomainException exception) {
            bankMetrics.incrementFailedTransfers();
            throw exception;
        }
    }

    private void ensureSourceAccountCanBeDebitedByCurrentActor(
            Account source, AuthenticatedActor actor) {
        if (actor != null && actor.isAdministrative()) {
            return;
        }

        if (actor != null
                && actor.hasRole("CUSTOMER")
                && source.getCustomerId().equals(actor.customerId())) {
            return;
        }

        throw new ForbiddenOperationException(
                "Authenticated customer cannot debit the requested source account");
    }

    private void ensureTransferCanBeViewedByCurrentActor(
            Account source, Account target, AuthenticatedActor actor) {
        if (actor != null && actor.isAdministrative()) {
            return;
        }

        if (actor != null
                && actor.hasRole("CUSTOMER")
                && (source.getCustomerId().equals(actor.customerId())
                        || target.getCustomerId().equals(actor.customerId()))) {
            return;
        }

        throw new ForbiddenOperationException(
                "Authenticated customer cannot access the requested transfer");
    }

    private TransferResult toResponse(Transfer transfer, Account source, Account target) {
        return new TransferResult(
                transfer.getTransferReference(),
                transfer.getSourceAccountId(),
                source.getAccountNumber(),
                transfer.getTargetAccountId(),
                target.getAccountNumber(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt());
    }

    private TransferIdempotencyPayload idempotencyPayload(CreateTransferCommand command) {
        return new TransferIdempotencyPayload(
                command.sourceAccountNumber(),
                command.targetAccountNumber(),
                command.amount(),
                command.currency());
    }

    private record TransferIdempotencyPayload(
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            Currency currency) {}
}
