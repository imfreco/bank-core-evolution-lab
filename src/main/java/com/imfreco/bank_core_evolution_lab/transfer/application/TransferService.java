package com.imfreco.bank_core_evolution_lab.transfer.application;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.audit.application.AuditService;
import com.imfreco.bank_core_evolution_lab.common.api.ActorContext;
import com.imfreco.bank_core_evolution_lab.common.config.BankMetrics;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CurrencyMismatchException;
import com.imfreco.bank_core_evolution_lab.common.exception.DomainException;
import com.imfreco.bank_core_evolution_lab.common.exception.InvalidTransferException;
import com.imfreco.bank_core_evolution_lab.common.exception.TransferNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.idempotency.IdempotencyService;
import com.imfreco.bank_core_evolution_lab.common.security.AuthenticatedUser;
import com.imfreco.bank_core_evolution_lab.movement.application.MovementService;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import com.imfreco.bank_core_evolution_lab.outbox.application.OutboxService;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import com.imfreco.bank_core_evolution_lab.transfer.infrastructure.TransferRepository;
import com.imfreco.bank_core_evolution_lab.transfer.web.TransferRequest;
import com.imfreco.bank_core_evolution_lab.transfer.web.TransferResponse;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public TransferService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            MovementService movementService,
            IdempotencyService idempotencyService,
            AuditService auditService,
            OutboxService outboxService,
            BankMetrics bankMetrics) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.movementService = movementService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.bankMetrics = bankMetrics;
    }

    @Transactional
    public TransferResponse create(
            TransferRequest request,
            String idempotencyKey,
            Principal principal,
            String channel,
            String correlationId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransferException("Idempotency-Key header is required");
        }
        validateSourceAccountOwnershipBeforeIdempotency(request.sourceAccountNumber(), principal);

        String actor = ActorContext.actor(principal);

        return idempotencyService
                .findCompletedResponseOrCreateRecord(
                        idempotencyKey.trim(), request, OPERATION_TYPE, TransferResponse.class)
                .orElseGet(
                        () ->
                                executeNewTransfer(
                                        request,
                                        idempotencyKey.trim(),
                                        principal,
                                        actor,
                                        channel,
                                        correlationId));
    }

    @Transactional(readOnly = true)
    public TransferResponse findByReference(String transferReference, Principal principal) {
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
        ensureTransferCanBeViewedByCurrentActor(source, target, principal);
        return toResponse(transfer, source, target);
    }

    private TransferResponse executeNewTransfer(
            TransferRequest request,
            String idempotencyKey,
            Principal principal,
            String actor,
            String channel,
            String correlationId) {
        try {
            TransferResponse response =
                    transferAtomically(
                            request, idempotencyKey, principal, actor, channel, correlationId);
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

    private TransferResponse transferAtomically(
            TransferRequest request,
            String idempotencyKey,
            Principal principal,
            String actor,
            String channel,
            String correlationId) {
        if (request.sourceAccountNumber().equals(request.targetAccountNumber())) {
            throw new InvalidTransferException("Source and target account must be different");
        }

        List<Account> lockedAccounts =
                accountRepository.findAllByAccountNumberInForUpdate(
                        List.of(request.sourceAccountNumber(), request.targetAccountNumber()));
        Map<String, Account> accountsByNumber =
                lockedAccounts.stream()
                        .collect(Collectors.toMap(Account::getAccountNumber, Function.identity()));

        Account source = accountOrThrow(accountsByNumber, request.sourceAccountNumber());
        Account target = accountOrThrow(accountsByNumber, request.targetAccountNumber());

        ensureSourceAccountCanBeDebitedByCurrentActor(source, principal);
        source.ensureActive();
        target.ensureActive();
        if (source.getCurrency() != target.getCurrency()
                || source.getCurrency() != request.currency()) {
            throw new CurrencyMismatchException();
        }

        Transfer transfer =
                transferRepository.save(
                        new Transfer(
                                source.getId(),
                                target.getId(),
                                request.amount(),
                                request.currency(),
                                idempotencyKey));

        source.debit(request.amount());
        target.credit(request.amount());

        movementService.create(
                source, transfer.getId(), MovementType.DEBIT, request.amount(), DESCRIPTION);
        movementService.create(
                target, transfer.getId(), MovementType.CREDIT, request.amount(), DESCRIPTION);

        transfer.complete();

        auditService.record(
                "TRANSFER_COMPLETED",
                "Transfer",
                transfer.getTransferReference(),
                actor,
                channel,
                correlationId,
                Map.of(
                        "sourceAccountId",
                        source.getId(),
                        "targetAccountId",
                        target.getId(),
                        "amount",
                        request.amount(),
                        "currency",
                        request.currency()));

        outboxService.create(
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
                        request.amount(),
                        "currency",
                        request.currency(),
                        "createdAt",
                        transfer.getCreatedAt(),
                        "correlationId",
                        correlationId));

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
            String sourceAccountNumber, Principal principal) {
        try {
            Account source =
                    accountRepository
                            .findByAccountNumber(sourceAccountNumber)
                            .orElseThrow(() -> new AccountNotFoundException(sourceAccountNumber));
            ensureSourceAccountCanBeDebitedByCurrentActor(source, principal);
        } catch (DomainException | AccessDeniedException exception) {
            bankMetrics.incrementFailedTransfers();
            throw exception;
        }
    }

    private void ensureSourceAccountCanBeDebitedByCurrentActor(
            Account source, Principal principal) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "OPERATOR")) {
            return;
        }

        AuthenticatedUser user = authenticatedUser(principal);
        if (user != null
                && hasRole(principal, "CUSTOMER")
                && source.getCustomerId().equals(user.customerId())) {
            return;
        }

        throw new AccessDeniedException(
                "Authenticated customer cannot debit the requested source account");
    }

    private void ensureTransferCanBeViewedByCurrentActor(
            Account source, Account target, Principal principal) {
        if (hasRole(principal, "ADMIN") || hasRole(principal, "OPERATOR")) {
            return;
        }

        AuthenticatedUser user = authenticatedUser(principal);
        if (user != null
                && hasRole(principal, "CUSTOMER")
                && (source.getCustomerId().equals(user.customerId())
                        || target.getCustomerId().equals(user.customerId()))) {
            return;
        }

        throw new AccessDeniedException(
                "Authenticated customer cannot access the requested transfer");
    }

    private boolean hasRole(Principal principal, String role) {
        AuthenticatedUser user = authenticatedUser(principal);
        if (user != null && user.roles().contains(role)) {
            return true;
        }

        Authentication authentication = authentication(principal);
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
    }

    private AuthenticatedUser authenticatedUser(Principal principal) {
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        Authentication authentication = authentication(principal);
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    private Authentication authentication(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return authentication;
        }
        return SecurityContextHolder.getContext().getAuthentication();
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
                transfer.getCompletedAt());
    }
}
