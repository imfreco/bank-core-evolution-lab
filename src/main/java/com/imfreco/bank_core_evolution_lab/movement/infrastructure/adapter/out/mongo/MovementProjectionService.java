package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.out.mongo;

import com.imfreco.bank_core_evolution_lab.movement.application.port.in.CustomerMovementViewResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.in.MovementResult;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementProjectionPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MovementProjectionService implements MovementProjectionPort {

    private static final Logger log = LoggerFactory.getLogger(MovementProjectionService.class);

    private final CustomerMovementViewRepository mongoRepository;

    public MovementProjectionService(CustomerMovementViewRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Optional<CustomerMovementViewResult> findView(UUID customerId) {
        try {
            return mongoRepository.findById(customerId.toString()).map(this::toResponse);
        } catch (RuntimeException exception) {
            log.warn(
                    "MongoDB movement view unavailable, falling back to SQL customerId={}",
                    customerId);
            return Optional.empty();
        }
    }

    @Override
    public void refreshCustomerMovements(UUID customerId, List<MovementResult> sqlMovements) {
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

    private CustomerMovementViewResult toResponse(CustomerMovementViewDocument document) {
        List<MovementResult> movements =
                document.getMovements().stream()
                        .map(
                                item ->
                                        new MovementResult(
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
        return new CustomerMovementViewResult(
                UUID.fromString(document.getCustomerId()),
                movements,
                document.getUpdatedAt(),
                "MONGODB_READ_MODEL");
    }
}
