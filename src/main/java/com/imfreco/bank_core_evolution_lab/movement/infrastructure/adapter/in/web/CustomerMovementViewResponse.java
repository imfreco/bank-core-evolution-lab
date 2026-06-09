package com.imfreco.bank_core_evolution_lab.movement.infrastructure.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerMovementViewResponse(
        UUID customerId, List<MovementResponse> movements, Instant updatedAt, String source) {}
