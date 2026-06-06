package com.imfreco.bank_core_evolution_lab.movement.application;

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

    private final MovementProjectionService projectionService;
    private final MovementService movementService;

    public MovementProjectionJob(
            MovementProjectionService projectionService, MovementService movementService) {
        this.projectionService = projectionService;
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
        for (var customerId : projectionService.findCustomerIdsWithAccounts()) {
            projectionService.refreshCustomerMovements(
                    customerId, movementService.findByCustomerFromSql(customerId));
        }
    }
}
