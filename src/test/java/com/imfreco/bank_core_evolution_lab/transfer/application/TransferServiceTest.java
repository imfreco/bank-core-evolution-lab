package com.imfreco.bank_core_evolution_lab.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.audit.application.port.out.AuditRecorderPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.BankMetricsPort;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.IdempotencyPort;
import com.imfreco.bank_core_evolution_lab.common.application.security.AuthenticatedActor;
import com.imfreco.bank_core_evolution_lab.common.exception.ForbiddenOperationException;
import com.imfreco.bank_core_evolution_lab.common.exception.InsufficientFundsException;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementRecorderPort;
import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventCreatorPort;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.CreateTransferCommand;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.TransferResult;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.out.TransferRepositoryPort;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import com.imfreco.bank_core_evolution_lab.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final TransferRepositoryPort transferRepository = mock(TransferRepositoryPort.class);
    private final MovementRecorderPort movementService = mock(MovementRecorderPort.class);
    private final IdempotencyPort idempotencyService = mock(IdempotencyPort.class);
    private final AuditRecorderPort auditService = mock(AuditRecorderPort.class);
    private final OutboxEventCreatorPort outboxService = mock(OutboxEventCreatorPort.class);
    private final BankMetricsPort bankMetrics = mock(BankMetricsPort.class);
    private final TransferService service =
            new TransferService(
                    accountRepository,
                    transferRepository,
                    movementService,
                    idempotencyService,
                    auditService,
                    outboxService,
                    bankMetrics);

    @Test
    void transfersMoneyAtomicallyAndCreatesAuditMovementAndOutbox() {
        CreateTransferCommand command =
                command("1000000001", "1000000002", new BigDecimal("10000.00"), Currency.COP);
        Account source = account("1000000001", CUSTOMER_ID, "100000.00");
        Account target = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(source));
        when(idempotencyService.findCompletedResponseOrCreateRecord(
                        eq("idem-1"), any(), eq("INTERNAL_TRANSFER"), eq(TransferResult.class)))
                .thenReturn(Optional.empty());
        when(accountRepository.findAllByAccountNumberInForUpdate(any()))
                .thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResult response = service.create(command);

        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(source.getAvailableBalance()).isEqualByComparingTo("90000.00");
        assertThat(target.getAvailableBalance()).isEqualByComparingTo("60000.00");
        verify(movementService)
                .create(
                        eq(source),
                        any(UUID.class),
                        eq(com.imfreco.bank_core_evolution_lab.movement.domain.MovementType.DEBIT),
                        eq(new BigDecimal("10000.00")),
                        eq("Transferencia interna"));
        verify(movementService)
                .create(
                        eq(target),
                        any(UUID.class),
                        eq(com.imfreco.bank_core_evolution_lab.movement.domain.MovementType.CREDIT),
                        eq(new BigDecimal("10000.00")),
                        eq("Transferencia interna"));
        verify(auditService)
                .record(
                        eq("TRANSFER_COMPLETED"),
                        eq("Transfer"),
                        any(),
                        eq("customer"),
                        eq("WEB"),
                        eq("corr-1"),
                        any());
        verify(outboxService).create(eq("Transfer"), any(), eq("TransferCompleted"), any());
        verify(idempotencyService).complete(eq("idem-1"), any(TransferResult.class));
        verify(bankMetrics).incrementSuccessfulTransfers();
    }

    @Test
    void returnsStoredIdempotentResponseWithoutDebitingAgain() {
        CreateTransferCommand command =
                command("1000000001", "1000000002", new BigDecimal("10000.00"), Currency.COP);
        TransferResult stored =
                new TransferResult(
                        "tx-1",
                        UUID.randomUUID(),
                        "1000000001",
                        UUID.randomUUID(),
                        "1000000002",
                        command.amount(),
                        Currency.COP,
                        TransferStatus.COMPLETED,
                        java.time.Instant.now(),
                        java.time.Instant.now());
        when(idempotencyService.findCompletedResponseOrCreateRecord(
                        eq("idem-1"), any(), eq("INTERNAL_TRANSFER"), eq(TransferResult.class)))
                .thenReturn(Optional.of(stored));
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(account("1000000001", CUSTOMER_ID, "100000.00")));

        TransferResult response = service.create(command);

        assertThat(response.transferReference()).isEqualTo("tx-1");
        verifyNoInteractions(transferRepository, movementService, auditService, outboxService);
    }

    @Test
    void failsWhenSourceHasInsufficientFunds() {
        CreateTransferCommand command =
                command("1000000001", "1000000002", new BigDecimal("1000000.00"), Currency.COP);
        when(idempotencyService.findCompletedResponseOrCreateRecord(
                        eq("idem-1"), any(), eq("INTERNAL_TRANSFER"), eq(TransferResult.class)))
                .thenReturn(Optional.empty());
        Account source = account("1000000001", CUSTOMER_ID, "100.00");
        Account target = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(source));
        when(accountRepository.findAllByAccountNumberInForUpdate(any()))
                .thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(InsufficientFundsException.class);
        verify(bankMetrics).incrementFailedTransfers();
    }

    @Test
    void customerCannotDebitAccountOwnedByAnotherCustomer() {
        CreateTransferCommand command =
                command("1000000002", "1000000001", new BigDecimal("10000.00"), Currency.COP);
        Account source = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(ForbiddenOperationException.class);

        verifyNoInteractions(idempotencyService, transferRepository, movementService, auditService);
        verify(bankMetrics).incrementFailedTransfers();
    }

    @Test
    void customerCannotReadTransferWhenNotParticipant() {
        Account source = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        Account target = account("1000000003", OTHER_CUSTOMER_ID, "25000.00");
        Transfer transfer =
                new Transfer(
                        source.getId(),
                        target.getId(),
                        new BigDecimal("10000.00"),
                        Currency.COP,
                        "idem-1");
        transfer.complete();
        when(transferRepository.findByTransferReference(transfer.getTransferReference()))
                .thenReturn(Optional.of(transfer));
        when(accountRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(
                        () ->
                                service.findByReference(
                                        transfer.getTransferReference(),
                                        customerActor(CUSTOMER_ID)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private CreateTransferCommand command(
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            Currency currency) {
        return new CreateTransferCommand(
                sourceAccountNumber,
                targetAccountNumber,
                amount,
                currency,
                "idem-1",
                customerActor(CUSTOMER_ID),
                "WEB",
                "corr-1");
    }

    private AuthenticatedActor customerActor(UUID customerId) {
        return new AuthenticatedActor("customer", List.of("CUSTOMER"), customerId);
    }

    private Account account(String accountNumber, UUID customerId, String balance) {
        return new Account(
                accountNumber,
                customerId,
                AccountType.SAVINGS,
                Currency.COP,
                new BigDecimal(balance));
    }
}
