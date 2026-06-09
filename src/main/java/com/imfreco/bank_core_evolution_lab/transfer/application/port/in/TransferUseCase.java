package com.imfreco.bank_core_evolution_lab.transfer.application.port.in;

import com.imfreco.bank_core_evolution_lab.common.application.security.AuthenticatedActor;

public interface TransferUseCase {

    TransferResult create(CreateTransferCommand command);

    TransferResult findByReference(String transferReference, AuthenticatedActor actor);
}
