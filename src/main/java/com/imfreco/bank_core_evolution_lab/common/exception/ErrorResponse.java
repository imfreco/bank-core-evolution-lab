package com.imfreco.bank_core_evolution_lab.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldErrorDetail> details) {}
