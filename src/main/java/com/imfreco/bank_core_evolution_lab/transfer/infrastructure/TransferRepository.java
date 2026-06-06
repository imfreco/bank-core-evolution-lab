package com.imfreco.bank_core_evolution_lab.transfer.infrastructure;

import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByTransferReference(String transferReference);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
