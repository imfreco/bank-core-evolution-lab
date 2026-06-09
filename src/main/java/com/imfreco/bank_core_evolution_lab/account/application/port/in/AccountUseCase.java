package com.imfreco.bank_core_evolution_lab.account.application.port.in;

import java.util.UUID;

public interface AccountUseCase {

    AccountResult create(CreateAccountCommand command);

    AccountResult get(UUID accountId);

    BalanceResult getBalance(UUID accountId);

    AccountResult block(UUID accountId, String actor, String channel, String correlationId);
}
