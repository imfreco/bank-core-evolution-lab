package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.in.scheduler;

import com.imfreco.bank_core_evolution_lab.account.application.port.out.AccountRepositoryPort;
import com.imfreco.bank_core_evolution_lab.movement.application.MovementService;
import com.imfreco.bank_core_evolution_lab.movement.application.port.out.MovementProjectionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        prefix = "bank.projections.mongodb",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MovementProjectionJob {

    private final MovementProjectionPort projectionPort;
    private final AccountRepositoryPort accountRepository;
    private final MovementService movementService;

    public MovementProjectionJob(
            MovementProjectionPort projectionPort,
            AccountRepositoryPort accountRepository,
            MovementService movementService) {
        this.projectionPort = projectionPort;
        this.accountRepository = accountRepository;
        this.movementService = movementService;
    }

    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "${bank.projections.mongodb.fixed-delay:15000}")
    public void refreshMongoReadModel() {
        /*
         * SQL/PostgreSQL es la fuente de verdad. MongoDB es una vista de lectura
         * eventualmente consistente, reconstruible por job de reconciliacion si una
         * actualizacion de proyeccion falla o llega tarde.
         */
        for (var customerId :
                accountRepository.findAll().stream()
                        .map(account -> account.getCustomerId())
                        .distinct()
                        .toList()) {
            projectionPort.refreshCustomerMovements(
                    customerId, movementService.findByCustomerFromSql(customerId));
        }
    }
}
