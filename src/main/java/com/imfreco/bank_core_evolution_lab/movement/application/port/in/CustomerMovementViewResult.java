package com.imfreco.bank_core_evolution_lab.movement.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerMovementViewResult(
        UUID customerId, List<MovementResult> movements, Instant updatedAt, String source) {}
