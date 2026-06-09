package com.imfreco.bank_core_evolution_lab.movement.application.port.out;

import com.imfreco.bank_core_evolution_lab.account.domain.Account;
import com.imfreco.bank_core_evolution_lab.movement.domain.Movement;
import com.imfreco.bank_core_evolution_lab.movement.domain.MovementType;
import java.math.BigDecimal;
import java.util.UUID;

public interface MovementRecorderPort {

    Movement create(
            Account account,
            UUID transferId,
            MovementType type,
            BigDecimal amount,
            String description);
}
