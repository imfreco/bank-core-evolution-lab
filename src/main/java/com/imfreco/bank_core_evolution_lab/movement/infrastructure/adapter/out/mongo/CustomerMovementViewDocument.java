package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.out.mongo;

import com.imfreco.bank_core_evolution_lab.account.domain.Currency;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "customer_movement_view")
public class CustomerMovementViewDocument {

    @Id private String id;
    private String customerId;
    private List<MovementItemDocument> movements = new ArrayList<>();
    private Instant updatedAt;

    protected CustomerMovementViewDocument() {}

    public CustomerMovementViewDocument(String customerId, List<MovementItemDocument> movements) {
        this.id = customerId;
        this.customerId = customerId;
        this.movements = movements;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<MovementItemDocument> getMovements() {
        return movements;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public record MovementItemDocument(
            String accountId,
            String transferReference,
            MovementType type,
            BigDecimal amount,
            Currency currency,
            String description,
            Instant createdAt,
            String channel) {}
}
