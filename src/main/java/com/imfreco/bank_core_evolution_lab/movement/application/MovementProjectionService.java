package com.imfreco.bank_core_evolution_lab.movement.application;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.account.infrastructure.AccountRepository;
import com.imfreco.bank_core_evolution_lab.movement.mongo.CustomerMovementViewDocument;
import com.imfreco.bank_core_evolution_lab.movement.mongo.CustomerMovementViewRepository;
import com.imfreco.bank_core_evolution_lab.movement.web.CustomerMovementViewResponse;
import com.imfreco.bank_core_evolution_lab.movement.web.MovementResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MovementProjectionService {

    private static final Logger log = LoggerFactory.getLogger(MovementProjectionService.class);

    private final CustomerMovementViewRepository mongoRepository;
    private final AccountRepository accountRepository;

    public MovementProjectionService(
            CustomerMovementViewRepository mongoRepository, AccountRepository accountRepository) {
        this.mongoRepository = mongoRepository;
        this.accountRepository = accountRepository;
    }

    public Optional<CustomerMovementViewResponse> findView(UUID customerId) {
        try {
            return mongoRepository.findById(customerId.toString()).map(this::toResponse);
        } catch (RuntimeException exception) {
            log.warn(
                    "MongoDB movement view unavailable, falling back to SQL customerId={}",
                    customerId);
            return Optional.empty();
        }
    }

    public void refreshCustomerMovements(UUID customerId, List<MovementResponse> sqlMovements) {
        try {
            List<CustomerMovementViewDocument.MovementItemDocument> items =
                    sqlMovements.stream()
                            .map(
                                    item ->
                                            new CustomerMovementViewDocument.MovementItemDocument(
                                                    item.accountId().toString(),
                                                    item.transferReference(),
                                                    item.type(),
                                                    item.amount(),
                                                    item.currency(),
                                                    item.description(),
                                                    item.createdAt(),
                                                    "WEB"))
                            .toList();
            mongoRepository.save(new CustomerMovementViewDocument(customerId.toString(), items));
        } catch (RuntimeException exception) {
            log.warn("Could not refresh MongoDB movement projection customerId={}", customerId);
        }
    }

    public List<UUID> findCustomerIdsWithAccounts() {
        return accountRepository.findAll().stream().map(Account::getCustomerId).distinct().toList();
    }

    private CustomerMovementViewResponse toResponse(CustomerMovementViewDocument document) {
        List<MovementResponse> movements =
                document.getMovements().stream()
                        .map(
                                item ->
                                        new MovementResponse(
                                                null,
                                                UUID.fromString(item.accountId()),
                                                item.transferReference(),
                                                item.type(),
                                                item.amount(),
                                                item.currency(),
                                                null,
                                                item.description(),
                                                item.createdAt()))
                        .toList();
        return new CustomerMovementViewResponse(
                UUID.fromString(document.getCustomerId()),
                movements,
                document.getUpdatedAt(),
                "MONGODB_READ_MODEL");
    }
}
