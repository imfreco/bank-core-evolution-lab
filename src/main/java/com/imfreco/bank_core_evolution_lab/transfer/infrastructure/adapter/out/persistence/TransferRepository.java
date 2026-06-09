package com.imfreco.bank_core_evolution_lab.transfer.infrastructure.adapter.out.persistence;

import com.imfreco.bank_core_evolution_lab.transfer.application.port.out.TransferRepositoryPort;
import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID>, TransferRepositoryPort {

    Optional<Transfer> findByTransferReference(String transferReference);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
