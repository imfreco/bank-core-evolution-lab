package com.imfreco.bank_core_evolution_lab.common.application.port.out;

import java.util.Optional;

public interface IdempotencyPort {

    <T> Optional<T> findCompletedResponseOrCreateRecord(
            String idempotencyKey, Object requestBody, String operationType, Class<T> responseType);

    void complete(String idempotencyKey, Object responseBody);

    String hashRequest(Object requestBody);
}
