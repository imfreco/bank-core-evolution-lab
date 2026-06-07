package com.imfreco.bank_core_evolution_lab.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.domain.AccountType;
import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.audit.application.AuditService;
import com.imfreco.bank_core_evolution_lab.common.config.BankMetrics;
import com.imfreco.bank_core_evolution_lab.common.exception.InsufficientFundsException;
import com.imfreco.bank_core_evolution_lab.common.idempotency.IdempotencyService;
import com.imfreco.bank_core_evolution_lab.common.security.AuthenticatedUser;
import com.imfreco.bank_core_evolution_lab.movement.application.MovementService;
import com.imfreco.bank_core_evolution_lab.outbox.application.OutboxService;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import com.imfreco.bank_core_evolution_lab.transfer.domain.TransferStatus;
import com.imfreco.bank_core_evolution_lab.transfer.infrastructure.TransferRepository;
import com.imfreco.bank_core_evolution_lab.transfer.web.TransferRequest;
import com.imfreco.bank_core_evolution_lab.transfer.web.TransferResponse;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class TransferServiceTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final TransferRepository transferRepository = mock(TransferRepository.class);
    private final MovementService movementService = mock(MovementService.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final OutboxService outboxService = mock(OutboxService.class);
    private final BankMetrics bankMetrics = mock(BankMetrics.class);
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
        TransferRequest request =
                new TransferRequest(
                        "1000000001", "1000000002", new BigDecimal("10000.00"), Currency.COP);
        Account source = account("1000000001", CUSTOMER_ID, "100000.00");
        Account target = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(source));
        when(idempotencyService.findCompletedResponseOrCreateRecord(
                        eq("idem-1"),
                        eq(request),
                        eq("INTERNAL_TRANSFER"),
                        eq(TransferResponse.class)))
                .thenReturn(Optional.empty());
        when(accountRepository.findAllByAccountNumberInForUpdate(any()))
                .thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response =
                service.create(request, "idem-1", customerPrincipal(CUSTOMER_ID), "WEB", "corr-1");

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
        verify(idempotencyService).complete(eq("idem-1"), any(TransferResponse.class));
        verify(bankMetrics).incrementSuccessfulTransfers();
    }

    @Test
    void returnsStoredIdempotentResponseWithoutDebitingAgain() {
        TransferRequest request =
                new TransferRequest(
                        "1000000001", "1000000002", new BigDecimal("10000.00"), Currency.COP);
        TransferResponse stored =
                new TransferResponse(
                        "tx-1",
                        UUID.randomUUID(),
                        "1000000001",
                        UUID.randomUUID(),
                        "1000000002",
                        request.amount(),
                        Currency.COP,
                        TransferStatus.COMPLETED,
                        java.time.Instant.now(),
                        java.time.Instant.now());
        when(idempotencyService.findCompletedResponseOrCreateRecord(
                        eq("idem-1"),
                        eq(request),
                        eq("INTERNAL_TRANSFER"),
                        eq(TransferResponse.class)))
                .thenReturn(Optional.of(stored));
        when(accountRepository.findByAccountNumber("1000000001"))
                .thenReturn(Optional.of(account("1000000001", CUSTOMER_ID, "100000.00")));

        TransferResponse response =
                service.create(request, "idem-1", customerPrincipal(CUSTOMER_ID), "WEB", "corr-1");

        assertThat(response.transferReference()).isEqualTo("tx-1");
        verifyNoInteractions(transferRepository, movementService, auditService, outboxService);
    }

    @Test
    void failsWhenSourceHasInsufficientFunds() {
        TransferRequest request =
                new TransferRequest(
                        "1000000001", "1000000002", new BigDecimal("1000000.00"), Currency.COP);
        when(idempotencyService.findCompletedResponseOrCreateRecord(
                        eq("idem-1"),
                        eq(request),
                        eq("INTERNAL_TRANSFER"),
                        eq(TransferResponse.class)))
                .thenReturn(Optional.empty());
        Account source = account("1000000001", CUSTOMER_ID, "100.00");
        Account target = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(source));
        when(accountRepository.findAllByAccountNumberInForUpdate(any()))
                .thenReturn(List.of(source, target));
        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        request,
                                        "idem-1",
                                        customerPrincipal(CUSTOMER_ID),
                                        "WEB",
                                        "corr-1"))
                .isInstanceOf(InsufficientFundsException.class);
        verify(bankMetrics).incrementFailedTransfers();
    }

    @Test
    void customerCannotDebitAccountOwnedByAnotherCustomer() {
        TransferRequest request =
                new TransferRequest(
                        "1000000002", "1000000001", new BigDecimal("10000.00"), Currency.COP);
        Account source = account("1000000002", OTHER_CUSTOMER_ID, "50000.00");
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(source));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        request,
                                        "idem-1",
                                        customerPrincipal(CUSTOMER_ID),
                                        "WEB",
                                        "corr-1"))
                .isInstanceOf(AccessDeniedException.class);

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
                                        customerPrincipal(CUSTOMER_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Principal customerPrincipal(UUID customerId) {
        return new AuthenticatedUser("customer", List.of("CUSTOMER"), customerId);
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
