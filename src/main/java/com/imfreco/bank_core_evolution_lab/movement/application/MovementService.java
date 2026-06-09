package com.imfreco.bank_core_evolution_lab.movement.application;

import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.application.port.out.CustomerRepositoryPort;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.CustomerMovementViewResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.MovementResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.MovementUseCase;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementProjectionPort;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementRecorderPort;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementRepositoryPort;
import com.imfreco.bank_core_evolution_lab.movement.domain.Movement;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.out.TransferRepositoryPort;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovementService implements MovementUseCase, MovementRecorderPort {

    private final MovementRepositoryPort movementRepository;
    private final AccountRepositoryPort accountRepository;
    private final CustomerRepositoryPort customerRepository;
    private final TransferRepositoryPort transferRepository;
    private final MovementProjectionPort movementProjection;

    public MovementService(
            MovementRepositoryPort movementRepository,
            AccountRepositoryPort accountRepository,
            CustomerRepositoryPort customerRepository,
            TransferRepositoryPort transferRepository,
            MovementProjectionPort movementProjection) {
        this.movementRepository = movementRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transferRepository = transferRepository;
        this.movementProjection = movementProjection;
    }

    @Override
    public Movement create(
            Account account,
            UUID transferId,
            MovementType type,
            java.math.BigDecimal amount,
            String description) {
        return movementRepository.save(
                new Movement(
                        account.getId(),
                        transferId,
                        type,
                        amount,
                        account.getCurrency(),
                        account.getAvailableBalance(),
                        description));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResult> findByAccount(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId.toString());
        }
        return toResponses(movementRepository.findByAccountIdOrderByCreatedAtDesc(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerMovementViewResult findByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }
        return movementProjection
                .findView(customerId)
                .orElseGet(
                        () ->
                                new CustomerMovementViewResult(
                                        customerId,
                                        findByCustomerFromSql(customerId),
                                        Instant.now(),
                                        "SQL_FALLBACK"));
    }

    @Transactional(readOnly = true)
    public List<MovementResult> findByCustomerFromSql(UUID customerId) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        List<UUID> accountIds = accounts.stream().map(Account::getId).toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return toResponses(movementRepository.findByAccountIdInOrderByCreatedAtDesc(accountIds));
    }

    public List<MovementResult> toResponses(Collection<Movement> movements) {
        Map<UUID, Transfer> transfers =
                transferRepository
                        .findAllById(
                                movements.stream()
                                        .map(Movement::getTransferId)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Transfer::getId, Function.identity()));
        return movements.stream()
                .map(movement -> toResponse(movement, transfers.get(movement.getTransferId())))
                .toList();
    }

    private MovementResult toResponse(Movement movement, Transfer transfer) {
        return new MovementResult(
                movement.getId(),
                movement.getAccountId(),
                transfer == null ? null : transfer.getTransferReference(),
                movement.getType(),
                movement.getAmount(),
                movement.getCurrency(),
                movement.getBalanceAfterMovement(),
                movement.getDescription(),
                movement.getCreatedAt());
    }
}
