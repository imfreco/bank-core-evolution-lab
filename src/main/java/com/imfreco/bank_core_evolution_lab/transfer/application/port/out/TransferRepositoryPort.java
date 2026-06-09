package com.imfreco.bank_core_evolution_lab.transfer.application.port.out;

import com.imfreco.bank_core_evolution_lab.transfer.domain.Transfer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepositoryPort {

    Transfer save(Transfer transfer);

    Optional<Transfer> findById(UUID transferId);

    Optional<Transfer> findByTransferReference(String transferReference);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    List<Transfer> findAllById(Iterable<UUID> transferIds);
}
