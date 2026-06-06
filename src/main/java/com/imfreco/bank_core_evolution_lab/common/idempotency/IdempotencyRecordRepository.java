package com.imfreco.bank_core_evolution_lab.common.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
