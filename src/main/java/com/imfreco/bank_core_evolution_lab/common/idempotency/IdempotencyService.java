package com.imfreco.bank_core_evolution_lab.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imfreco.bank_core_evolution_lab.common.application.port.out.IdempotencyPort;
import com.imfreco.bank_core_evolution_lab.common.exception.DuplicateIdempotencyKeyException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService implements IdempotencyPort {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Optional<T> findCompletedResponseOrCreateRecord(
            String idempotencyKey,
            Object requestBody,
            String operationType,
            Class<T> responseType) {
        String requestHash = hashRequest(requestBody);
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new DuplicateIdempotencyKeyException(
                        "Idempotency-Key was already used with a different request body");
            }
            if (record.getStatus() == IdempotencyStatus.COMPLETED
                    && record.getResponseBody() != null) {
                return Optional.of(readResponse(record.getResponseBody(), responseType));
            }
            throw new DuplicateIdempotencyKeyException(
                    "Operation is already in progress for this Idempotency-Key");
        }

        try {
            repository.saveAndFlush(
                    new IdempotencyRecord(idempotencyKey, requestHash, operationType));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateIdempotencyKeyException(
                    "Operation is already in progress or completed for this Idempotency-Key");
        }
        return Optional.empty();
    }

    @Override
    public void complete(String idempotencyKey, Object responseBody) {
        IdempotencyRecord record =
                repository
                        .findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(
                                () ->
                                        new DuplicateIdempotencyKeyException(
                                                "Idempotency record not found"));
        record.complete(writeResponse(responseBody));
    }

    @Override
    public String hashRequest(Object requestBody) {
        try {
            String canonicalJson = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not hash idempotent request", exception);
        }
    }

    private <T> T readResponse(String responseBody, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Stored idempotent response could not be deserialized", exception);
        }
    }

    private String writeResponse(Object responseBody) {
        try {
            return objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize idempotent response", exception);
        }
    }
}
