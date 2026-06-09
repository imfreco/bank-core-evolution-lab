package com.imfreco.bank_core_evolution_lab.outbox.infrastructure.adapter.out.publisher;

import com.imfreco.bank_core_evolution_lab.outbox.application.port.out.OutboxEventRepositoryPort;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEvent;
import com.imfreco.bank_core_evolution_lab.outbox.domain.OutboxEventStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        prefix = "bank.outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxEventRepositoryPort repository;

    public OutboxPublisherJob(OutboxEventRepositoryPort repository) {
        this.repository = repository;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${bank.outbox.publisher.fixed-delay:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);
        for (OutboxEvent event : events) {
            try {
                // Simulacion: aqui se integraria Kafka, RabbitMQ, SNS/SQS u otro broker
                // transaccionalmente desacoplado.
                log.info(
                        "Publishing outbox event id={} type={} aggregateId={}",
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId());
                event.markPublished();
            } catch (RuntimeException exception) {
                event.markFailed();
                log.warn("Outbox event publication failed id={}", event.getId(), exception);
            }
        }
    }
}
