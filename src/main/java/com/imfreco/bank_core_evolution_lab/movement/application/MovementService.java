package com.imfreco.bank_core_evolution_lab.movement.application;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.common.exception.AccountNotFoundException;
import com.imfreco.bank_core_evolution_lab.common.exception.CustomerNotFoundException;
import com.imfreco.bank_core_evolution_lab.customer.infrastructure.CustomerRepository;
import com.imfreco.bank_core_evolution_lab.movement.domain.Movement;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import com.imfreco.bank_core_evolution_lab.movement.infrastructure.MovementRepository;
import com.imfreco.bank_core_evolution_lab.movement.web.CustomerMovementViewResponse;
import com.imfreco.bank_core_evolution_lab.movement.web.MovementResponse;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import com.imfreco.bank_core_evolution_lab.transfer.infrastructure.TransferRepository;
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
public class MovementService {

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransferRepository transferRepository;
    private final MovementProjectionService movementProjectionService;

    public MovementService(
            MovementRepository movementRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransferRepository transferRepository,
            MovementProjectionService movementProjectionService) {
        this.movementRepository = movementRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transferRepository = transferRepository;
        this.movementProjectionService = movementProjectionService;
    }

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

    @Transactional(readOnly = true)
    public List<MovementResponse> findByAccount(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId.toString());
        }
        return toResponses(movementRepository.findByAccountIdOrderByCreatedAtDesc(accountId));
    }

    @Transactional(readOnly = true)
    public CustomerMovementViewResponse findByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }
        return movementProjectionService
                .findView(customerId)
                .orElseGet(
                        () ->
                                new CustomerMovementViewResponse(
                                        customerId,
                                        findByCustomerFromSql(customerId),
                                        Instant.now(),
                                        "SQL_FALLBACK"));
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findByCustomerFromSql(UUID customerId) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        List<UUID> accountIds = accounts.stream().map(Account::getId).toList();
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return toResponses(movementRepository.findByAccountIdInOrderByCreatedAtDesc(accountIds));
    }

    public List<MovementResponse> toResponses(Collection<Movement> movements) {
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

    private MovementResponse toResponse(Movement movement, Transfer transfer) {
        return new MovementResponse(
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
