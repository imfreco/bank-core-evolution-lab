package com.imfreco.bank_core_evolution_lab.transfer.infrastructure.adapter.in.web;

import com.imfreco.bank_core_evolution_lab.common.application.security.AuthenticatedActor;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.CreateTransferCommand;
import com.imfreco.bank_core_evolution_lab.transfer.application.port.in.TransferResult;

public final class TransferMapper {

    private TransferMapper() {}

    public static CreateTransferCommand toCommand(
            TransferRequest request,
            String idempotencyKey,
            AuthenticatedActor actor,
            String channel,
            String correlationId) {
        return new CreateTransferCommand(
                request.sourceAccountNumber(),
                request.targetAccountNumber(),
                request.amount(),
                request.currency(),
                idempotencyKey,
                actor,
                channel,
                correlationId);
    }

    public static TransferResponse toResponse(TransferResult result) {
        return new TransferResponse(
                result.transferReference(),
                result.sourceAccountId(),
                result.sourceAccountNumber(),
                result.targetAccountId(),
                result.targetAccountNumber(),
                result.amount(),
                result.currency(),
                result.status(),
                result.createdAt(),
                result.completedAt());
    }
}
